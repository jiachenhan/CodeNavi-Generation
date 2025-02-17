from __future__ import annotations
from abc import ABC, abstractmethod
from typing import TYPE_CHECKING

from app.abs.selected_topdown.history import ElementHistory
from app.abs.selected_topdown.prompts import TASK_DESCRIPTION_PROMPT, SELECT_STMT_PROMPT, SELECT_ELEMENT_PROMPT, \
    AFTER_TREE_TASK_PROMPT, STRUCTURE_ELEMENT_PROMPT

if TYPE_CHECKING:
    from app.abs.selected_topdown.inference import Analyzer
from app.basic_modification_analysis import background_analysis
from utils.config import LoggerConfig


_logger = LoggerConfig.get_logger(__name__)

STRUCTURE_RELATED_AST_NODE_TYPES = ["MoBlock", "MoDoStatement", "MoEnhancedForStatement", "MoForStatement",
                                    "MoIfStatement", "MoSwitchStatement", "MoSynchronizedStatement",
                                    "MoWhileStatement", "MoTryStatement", "MoTypeDeclarationStatement"]

NAME_AST_NODE_TYPES = ["MoSimpleName", "MoQualifiedName",
                       "MoBooleanLiteral", "MoCharacterLiteral", "MoNullLiteral",
                       "MoNumberLiteral", "MoStringLiteral", "MoTypeLiteral"]

class PromptState(ABC):
    def __init__(self, analyzer: Analyzer):
        self.analyzer = analyzer

    @abstractmethod
    def accept(self):
        pass

class InitialState(PromptState):
    def accept(self):
        self.analyzer.prompt_state = BackGroundState(self.analyzer)


class ExitState(PromptState):
    def accept(self):
        pass


class BackGroundState(PromptState):
    def task_prompt(self):
        _background_message = self.analyzer.global_history.get_background_history()
        _task_prompt = [{"role": "user", "content": TASK_DESCRIPTION_PROMPT}]
        _background_message.extend(_task_prompt)
        _task_response = self.analyzer.llm.invoke(_background_message)
        _task_prompt.append({"role": "assistant", "content": _task_response})
        self.analyzer.global_history.task_history = _task_prompt

    def accept(self):
        _background_message = background_analysis(self.analyzer.llm, self.analyzer.pattern_input)
        self.analyzer.global_history.background_history = _background_message
        self.task_prompt()
        self.analyzer.prompt_state = StmtState(self.analyzer)

class StmtState(PromptState):
    @staticmethod
    def tag_statements_prompt(stmts) -> (str, dict):
        tagged_stmts = [f"<{index}>{stmt.get('value')}</{index}>" for index, stmt in enumerate(stmts, 1)]
        mapping = {index: stmt for index, stmt in enumerate(stmts, 1)}
        return SELECT_STMT_PROMPT.format(stmt_list_prompt="\n".join(tagged_stmts)), mapping

    def accept(self):
        stmts = Analyzer.get_top_stmts_from_tree(self.analyzer.pattern_input.tree)
        stmt_list_prompt, tag_mapping = self.tag_statements_prompt(stmts)
        _task_message = self.analyzer.global_history.merge_before_task_history()
        _select_stmts_message = [{"role": "user", "content": stmt_list_prompt}]
        _task_message.extend(_select_stmts_message)
        for _ in range(self.analyzer.retries):
            _select_stmts_response = self.analyzer.llm.invoke(_task_message)
            valid, considered_stmts_index =  Analyzer.get_considered_nodes(_select_stmts_response)
            if not valid:
                _logger.warning(f"Invalid response: {_select_stmts_response}, retry! ")
                continue
            _select_stmts_message.append({"role": "assistant", "content": _select_stmts_response})
            self.analyzer.global_history.select_stmts_history = _select_stmts_message
            for index in considered_stmts_index:
                _element = tag_mapping.get(index)
                _element_id = _element.get("id")
                self.analyzer.considered_elements.add(_element_id)
                _element_history = self.analyzer.global_history.merge_stmts_history()
                self.analyzer.global_history.element_histories[_element_id] = ElementHistory(
                    element_id = _element_id,
                    history=_element_history
                )
                self.analyzer.element_stack.append(_element)
            self.analyzer.prompt_state = ElementState(self.analyzer)
            break

def tag_elements_prompt(element) -> (str, dict):
    _children = element.get("children")
    _tagged_children = [f"<{index}>{child.get('value')}</{index}>" for index, child in enumerate(_children, 1)]
    _tag_mapping = {index: child for index, child in enumerate(_children, 1)}
    return SELECT_ELEMENT_PROMPT.format(element_list_prompt="\n".join(_tagged_children)), _tag_mapping

def structure_analysis(_element, _analyzer) -> bool:
    _element_history = _analyzer.global_history.element_histories.get(_element.get("id"))
    _task_message = _element_history.get_history()
    _structure_prompt = STRUCTURE_ELEMENT_PROMPT.format(elementType=_element.get("type"),
                                                        element=_element.get("value"))
    _structure_message = [{"role": "user", "content": _structure_prompt}]
    _task_message.extend(_structure_message)
    for _ in range(_analyzer.retries):
        _structure_response = _analyzer.llm.invoke(_task_message)
        valid, consider = Analyzer.single_consider(_structure_response)
        if not valid:
            _logger.warning(f"Invalid response: {_structure_response}, retry! ")
            continue
        _structure_message.append({"role": "assistant", "content": _structure_response})
        _element_history.round = _structure_message
        return consider
    return False


class ElementState(PromptState):
    def analyze(self):
        _element = self.analyzer.current_element
        _element_id = _element.get("id")
        # 如果是name类型的节点，不再进行提问，因为上一级已经认为它是代表代码
        if _element.get("leaf"):
            return

        _element_history = self.analyzer.global_history.element_histories[_element_id]
        if _element.get("type") in STRUCTURE_RELATED_AST_NODE_TYPES:
            structure_crucial = structure_analysis(_element, self.analyzer)
            if structure_crucial:
                self.analyzer.considered_elements.remove(_element_id)

        _element_list_prompt, _tag_mapping = tag_elements_prompt(_element)
        _task_message = _element_history.get_history()
        _select_element_message = [{"role": "user", "content": _element_list_prompt}]
        _task_message.extend(_select_element_message)
        for _ in range(self.analyzer.retries):
            _select_elements_response = self.analyzer.llm.invoke(_task_message)
            valid, considered_stmts_index =  Analyzer.get_considered_nodes(_select_elements_response)
            if not valid:
                _logger.warning(f"Invalid response: {_select_elements_response}, retry! ")
                continue
            _select_element_message.append({"role": "assistant", "content": _select_elements_response})
            for index in considered_stmts_index:
                _child_element = _tag_mapping.get(index)
                _child_element_id = _child_element.get("id")
                self.analyzer.global_history.element_histories[_element_id].round = _select_element_message
                self.analyzer.considered_elements.add(_child_element_id)
                self.analyzer.element_stack.append(_child_element)
                self.analyzer.global_history.element_histories[_child_element_id] = ElementHistory(
                    element_id = _child_element_id,
                    history=self.analyzer.global_history.element_histories[_element_id].get_round_history()
                )
            break

    def accept(self):
        if len(self.analyzer.element_stack) > 0:
            self.analyzer.current_element = self.analyzer.element_stack.pop()
            self.analyze()
        else:
            self.analyzer.prompt_state = InsertState(self.analyzer)


class InsertState(PromptState):
    def analyze(self):
        _element = self.analyzer.current_element
        _element_id = _element.get("id")
        if _element.get("leaf"):
            return

        _element_list_prompt, _tag_mapping = tag_elements_prompt(_element)
        _task_message = self.analyzer.global_history.after_element_histories[_element_id].get_history()
        _select_element_message = [{"role": "user", "content": _element_list_prompt}]
        _task_message.extend(_select_element_message)
        for _ in range(self.analyzer.retries):
            _select_elements_response = self.analyzer.llm.invoke(_task_message)
            valid, considered_stmts_index =  Analyzer.get_considered_nodes(_select_elements_response)
            if not valid:
                _logger.warning(f"Invalid response: {_select_elements_response}, retry! ")
                continue
            _select_element_message.append({"role": "assistant", "content": _select_elements_response})
            for index in considered_stmts_index:
                _child_element = _tag_mapping.get(index)
                _child_element_id = _child_element.get("id")
                self.analyzer.considered_inserts.setdefault(self.analyzer.current_action_node.get("id"), []).append(_child_element_id)
                self.analyzer.global_history.after_element_histories[_element_id].round = _select_element_message
                self.analyzer.element_stack.append(_child_element)
                self.analyzer.global_history.after_element_histories[_child_element_id] = ElementHistory(
                    element_id = _child_element_id,
                    history=self.analyzer.global_history.after_element_histories[_element_id].get_round_history()
                )
            break

    def task_prompt(self):
        pass
        _background_message = self.analyzer.global_history.get_background_history()
        _task_prompt = [{"role": "user", "content": AFTER_TREE_TASK_PROMPT}]
        _background_message.extend(_task_prompt)
        _task_response = self.analyzer.llm.invoke(_background_message)
        _task_prompt.append({"role": "assistant", "content": _task_response})
        self.analyzer.global_history.after_task_history = _task_prompt

    def accept(self):
        if len(self.analyzer.element_stack) > 0:
            self.analyzer.current_element = self.analyzer.element_stack.pop()
            self.analyze()
        else:
            if len(self.analyzer.insert_nodes) > 0:
                _insert_node = self.analyzer.insert_nodes.pop()
                self.analyzer.current_action_node = _insert_node
                self.analyzer.element_stack.append(_insert_node)
                if not self.analyzer.global_history.after_task_history:
                    self.task_prompt()
                self.analyzer.global_history.after_element_histories[_insert_node.get("id")] = ElementHistory(
                    element_id = _insert_node.get("id"),
                    history = self.analyzer.global_history.merge_after_task_history()
                )
            else:
                self.analyzer.prompt_state = MoveState(self.analyzer)


class MoveState(PromptState):
    def analyze(self):
        _element = self.analyzer.current_element
        _element_id = _element.get("id")
        if _element.get("leaf"):
            return

        _element_list_prompt, _tag_mapping = tag_elements_prompt(_element)
        _task_message = self.analyzer.global_history.after_element_histories[_element_id].get_history()
        _select_element_message = [{"role": "user", "content": _element_list_prompt}]
        _task_message.extend(_select_element_message)
        for _ in range(self.analyzer.retries):
            _select_elements_response = self.analyzer.llm.invoke(_task_message)
            valid, considered_stmts_index =  Analyzer.get_considered_nodes(_select_elements_response)
            if not valid:
                _logger.warning(f"Invalid response: {_select_elements_response}, retry! ")
                continue
            _select_element_message.append({"role": "assistant", "content": _select_elements_response})
            for index in considered_stmts_index:
                _child_element = _tag_mapping.get(index)
                _child_element_id = _child_element.get("id")
                self.analyzer.considered_moves.setdefault(self.analyzer.current_action_node.get("id"), []).append(_child_element_id)
                self.analyzer.global_history.after_element_histories[_element_id].round = _select_element_message
                self.analyzer.element_stack.append(_child_element)
                self.analyzer.global_history.after_element_histories[_child_element_id] = ElementHistory(
                    element_id = _child_element_id,
                    history=self.analyzer.global_history.after_element_histories[_element_id].get_round_history()
                )
            break

    def accept(self):
        if len(self.analyzer.element_stack) > 0:
            self.analyzer.current_element = self.analyzer.element_stack.pop()
            self.analyze()
        else:
            if len(self.analyzer.move_parent_nodes) > 0:
                _move_node = self.analyzer.move_parent_nodes.pop()
                self.analyzer.current_action_node = _move_node
                self.analyzer.element_stack.append(_move_node)
                self.analyzer.global_history.after_element_histories[_move_node.get("id")] = ElementHistory(
                    element_id = _move_node.get("id"),
                    history = self.analyzer.global_history.merge_after_task_history()
                )
            else:
                self.analyzer.prompt_state = ExitState(self.analyzer)
