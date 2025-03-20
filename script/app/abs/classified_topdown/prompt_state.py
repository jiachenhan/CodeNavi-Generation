import ast
import copy
import re
from typing import Optional

from app.basic_modification_analysis import background_analysis
from app.abs.classified_topdown.history import ElementHistory
from app.abs.classified_topdown.prompts import NORMAL_ELEMENT_PROMPT, NAME_ELEMENT_PROMPT, STRUCTURE_ELEMENT_PROMPT, \
    TASK_DESCRIPTION_PROMPT, \
    AFTER_TREE_TASK_PROMPT, AFTER_TREE_ELEMENT_PROMPT, AFTER_TREE_NAME_PROMPT, REGEX_NAME_PROMPT, \
    ROUGH_SELECT_LINES_PROMPT, LITERAL_ELEMENT_PROMPT, REGEX_LITERAL_PROMPT, NORMAL_TOP_ELEMENT_PROMPT, \
    AFTER_SELECT_LINES_PROMPT
from utils.common import retry_times, valid_with
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)

STRUCTURE_RELATED_AST_NODE_TYPES = ["MoBlock", "MoDoStatement", "MoEnhancedForStatement", "MoForStatement",
                                    "MoIfStatement", "MoSwitchStatement", "MoSynchronizedStatement",
                                    "MoWhileStatement", "MoTryStatement", "MoTypeDeclarationStatement"]

NAME_AST_NODE_TYPES = ["MoSimpleName", "MoQualifiedName",
                       "MoBooleanLiteral", "MoCharacterLiteral", "MoNullLiteral",
                       "MoNumberLiteral", "MoStringLiteral", "MoTypeLiteral"]

class PromptState:
    def __init__(self, analyzer):
        self.analyzer = analyzer

    def accept(self):
        raise NotImplementedError

class InitialState(PromptState):
    def accept(self):
        self.analyzer.prompt_state = BackGroundState(self.analyzer)


class ExitState(PromptState):
    def accept(self):
        pass


class BackGroundState(PromptState):
    def task_prompt(self):
        _background_messages_copy = copy.deepcopy(self.analyzer.global_history.background_history)
        task_prompt = [{"role": "user", "content": TASK_DESCRIPTION_PROMPT}]
        _background_messages_copy.extend(task_prompt)
        _background_response2 = self.analyzer.llm.invoke(_background_messages_copy)
        task_prompt.append({"role": "assistant", "content": _background_response2})
        self.analyzer.global_history.task_history = task_prompt

    def accept(self):
        background_history = background_analysis(self.analyzer.llm, self.analyzer.pattern_input)
        self.analyzer.global_history.background_history = background_history
        self.task_prompt()
        self.analyzer.prompt_state = AttentionLineState(self.analyzer)


class AttentionLineState(PromptState):
    """This state allows the model to roughly select important rows"""
    pattern = re.compile(r"""
        # 匹配固定起始标记[critical lines]
        \[critical\s*lines]  
        \s*                  # 允许起始标记后的任意空白（包括换行）
        \|\|\|               # 匹配第一个分隔符
        \s*
        # 捕获关键行号列表部分
        (                    # 开始捕获组group(1)
          \[\s*(?:\d+(?:\s*,\s*\d+)*)?\s*]
        )                    # 结束捕获组
        \s*                  # 允许列表后的空白
        \|\|\|               # 匹配第二个分隔符
        [\s\S]*              # 匹配后续所有内容（分析部分）
    """, re.VERBOSE | re.IGNORECASE)

    def init_history(self):
        for element in self.analyzer.element_stack:
            element_id = element.get("id")
            background_history_copy = copy.deepcopy(self.analyzer.global_history.background_history)
            background_history_copy.extend(self.analyzer.global_history.task_history)
            background_history_copy.extend(self.analyzer.global_history.roughly_line_history)
            self.analyzer.global_history.element_histories[element_id] = ElementHistory(element_id=element_id,
                                                                                        history=background_history_copy)

    def check_valid(self, response: str) -> bool:
        match = re.search(self.pattern, response)
        return bool(match)

    def get_attention_lines(self, response: str) -> list:
        match = re.search(self.pattern, response)
        try:
            return ast.literal_eval(match.group(1))  # 直接返回列表
        except SyntaxError:
            _logger.error(f"can't trans to list: {match.group(1)}")
            return []

    @retry_times(retries=5)
    @valid_with(check_valid)
    def invoke_validate_retry(self, messages: list) -> str:
        return self.analyzer.llm.invoke(messages)

    def accept(self):
        _background_messages_copy = copy.deepcopy(self.analyzer.global_history.background_history)
        _select_lines_prompt = [{"role": "user", "content": ROUGH_SELECT_LINES_PROMPT}]
        _background_messages_copy.extend(_select_lines_prompt)
        valid, response = self.invoke_validate_retry(_background_messages_copy)
        if valid:
            _select_lines_prompt.append({"role": "assistant", "content": response})
            self.analyzer.global_history.roughly_line_history = _select_lines_prompt
            attention_lines = self.get_attention_lines(response)
            self.analyzer.important_lines = attention_lines

        self.init_history()
        self.analyzer.prompt_state = ElementState(self.analyzer)
        return


class ElementState(PromptState):
    def accept(self):
        if len(self.analyzer.element_stack) > 0:
            self.analyzer.element_analysis()
            self.analyzer.last_state = self.__class__
        else:
            self.analyzer.prompt_state = AfterNodeState(self.analyzer)
            # self.analyzer.prompt_state = ExitState(self.analyzer)


class NormalElementState(PromptState):
    def accept(self):
        _element = self.analyzer.current_element
        # 如果这个元素不在重要行(+-1,考虑紧密上下文)中，则跳过
        start_line, end_line = _element.get("startLine"), _element.get("endLine")
        if not any(start_line - 1 <= num <= end_line + 1
                   for num in self.analyzer.important_lines):
            self.analyzer.prompt_state = ElementState(self.analyzer)
            return

        _parent = self.analyzer.child_parent_map.get(_element.get("id"))
        if self.analyzer.pattern_input.error_info:
            _error_info = self.analyzer.pattern_input.error_info
        else:
            _error_info = "Unknown Violation"

        _element_type = _element.get("type")
        if _parent is None:
            _element_prompt = NORMAL_TOP_ELEMENT_PROMPT.format(line=_element.get("startLine"),
                                                                element=_element.get("value"),
                                                                elementType=_element_type,
                                                                error_info=_error_info)
        else:
            _element_prompt = NORMAL_ELEMENT_PROMPT.format(line=_element.get("startLine"),
                                                           element=_element.get("value"),
                                                           elementType=_element_type,
                                                           parentElement=_parent.get("value"),
                                                           error_info=_error_info)
        _element_history = self.analyzer.get_current_element_history()
        _element_his_copy = copy.deepcopy(_element_history)
        _element_his_copy.add_user_message_to_history(_element_prompt)
        _round_prompt = _element_his_copy.history
        valid, response = self.analyzer.invoke_classify_retry(_round_prompt)
        if valid:
            _element_history.add_user_message_to_round(_element_prompt)
            _element_history.add_assistant_message_to_round(response)
            classified_res = self.analyzer.check_classified_num_response(response)
            if classified_res == 0:
                _logger.error(f"should not happened")
            elif classified_res == 1:
                self.analyzer.push(_element)
                self.analyzer.considered_elements.add(_element.get("id"))
            elif classified_res == 2:
                self.analyzer.push(_element)
        else:
            _logger.error(f"Invalid response: {response} After retry {self.analyzer.retries} times")

        self.analyzer.prompt_state = ElementState(self.analyzer)
        return


class NameState(PromptState):
    def accept(self):
        _element = self.analyzer.current_element
        if _element.get("type") in ("MoSimpleName", "MoQualifiedName"):
            _element_prompt = NAME_ELEMENT_PROMPT.format(line=_element.get("startLine"),
                                                           element=_element.get("value"))
        else:
            _element_prompt = LITERAL_ELEMENT_PROMPT.format(line=_element.get("startLine"),
                                                           element=_element.get("value"))

        _element_history = self.analyzer.get_current_element_history()
        _element_his_copy = copy.deepcopy(_element_history)
        _element_his_copy.add_user_message_to_history(_element_prompt)
        _round_prompt = _element_his_copy.history
        valid, response = self.analyzer.invoke_validate_retry(_round_prompt)

        if valid:
            _element_history.add_user_message_to_round(_element_prompt)
            _element_history.add_assistant_message_to_round(response)
            if self.analyzer.check_true_response(response):
                self.analyzer.considered_elements.add(_element.get("id"))
                self.analyzer.prompt_state = RegEXState(self.analyzer)
                return
        else:
            _logger.error(f"Invalid response: {response} After retry {self.analyzer.retries} times")
        self.analyzer.prompt_state = ElementState(self.analyzer)
        return


class RegEXState(PromptState):
    pattern = re.compile(r'''
        # 格式1："yes" ，捕获中间的内容
        \s*(yes)\s*             # 匹配并捕获 "yes"
        \|\|\|                  # 分隔符 "|||"
        \s*                     # 空格
        (                       # 捕获组：中间内容（允许转义字符）
            (?:                 # 非捕获组（循环结构）
                \\\|            # 转义的 |（必须优先匹配）
                |               # 或
                \|(?!\|\|)      # 单独的 |（后面不能紧跟 ||）
                |
                [^|\\]          # 非 | 且非反斜杠的普通字符
                |
                \\.             # 其他转义字符（如 \n, \t）
            )+                  # 重复1次或多次
        )
        \s*                     # 空格
        \|\|\|                  # 分隔符 "|||"
        
        |                       # 或
    
        # 格式2："no" ，固定内容 "None"
        \s*(no)\s*              # 匹配并捕获 "no"
        \|\|\|                  # 分隔符 "|||"
        \s*                     # 空格
        None                    # 固定内容 "None"（非捕获组）
        \s*                     # 空格
        \|\|\|                  # 分隔符 "|||"
    ''', re.VERBOSE)

    def check_valid(self, response: str) -> bool:
        parts = response.rsplit('\n', 1)
        part = parts[0] if len(parts) > 1 else response
        match = re.search(self.pattern, part.strip())
        return bool(match)

    @retry_times(5)
    @valid_with(check_valid)
    def invoke_validate_retry(self, messages) -> str:
        return self.analyzer.llm.invoke(messages)

    def get_regex(self, response: str) -> (bool, Optional[str]):
        parts = response.rsplit('\n', 1)
        part = parts[0] if len(parts) > 1 else response
        match = re.search(self.pattern, part.strip())
        if match:
            if match.group(1) and match.group(1).lower() == "yes":
                return True, match.group(2).strip()
            else:
                return False, None
        else:
            _logger.error(f"should not happened after check valid")
            return False, None

    def accept(self):
        _element = self.analyzer.current_element
        if _element.get("type") in ("MoSimpleName", "MoQualifiedName"):
            _reg_prompt = REGEX_NAME_PROMPT.format(value=_element.get("value"))
        else:
            _reg_prompt = REGEX_LITERAL_PROMPT.format(value=_element.get("value"))

        _element_history = self.analyzer.get_current_element_history()

        _element_his_copy = copy.deepcopy(_element_history)
        _element_his_copy.add_user_message_to_history(_reg_prompt)
        _round_prompt = _element_his_copy.history

        valid, response = self.invoke_validate_retry(_round_prompt)

        if valid:
            _element_history.regex_round.append({"role": "user", "content": _reg_prompt})
            _element_history.regex_round.append({"role": "assistant", "content": response})
            has_reg, regex = self.get_regex(response)
            if has_reg:
                self.analyzer.regex_map[_element.get("id")] = regex
        if isinstance(self.analyzer.last_state, InsertNodeState):
            self.analyzer.prompt_state = InsertNodeState(self.analyzer)
        elif isinstance(self.analyzer.last_state, MoveNodeState):
            self.analyzer.prompt_state = MoveNodeState(self.analyzer)
        else:
            self.analyzer.prompt_state = ElementState(self.analyzer)
        return


class AfterNodeState(PromptState):
    pattern = re.compile(r"""
        # 匹配固定起始标记[critical lines]
        \[critical\s*lines]  
        \s*                  # 允许起始标记后的任意空白（包括换行）
        \|\|\|               # 匹配第一个分隔符
        \s*
        # 捕获关键行号列表部分
        (                    # 开始捕获组group(1)
          \[\s*(?:\d+(?:\s*,\s*\d+)*)?\s*]
        )                    # 结束捕获组
        \s*                  # 允许列表后的空白
        \|\|\|               # 匹配第二个分隔符
        [\s\S]*              # 匹配后续所有内容（分析部分）
    """, re.VERBOSE | re.IGNORECASE)

    def after_task_prompt(self):
        _background_messages_copy = copy.deepcopy(self.analyzer.global_history.background_history)
        _after_task_prompt = [{"role": "user", "content": AFTER_TREE_TASK_PROMPT}]
        _background_messages_copy.extend(_after_task_prompt)
        _background_response2 = self.analyzer.llm.invoke(_background_messages_copy)
        _after_task_prompt.append({"role": "assistant", "content": _background_response2})
        self.analyzer.global_history.after_task_history = _after_task_prompt

    def check_valid(self, response: str) -> bool:
        match = re.search(self.pattern, response)
        return bool(match)

    def get_attention_lines(self, response: str) -> list:
        match = re.search(self.pattern, response)
        try:
            return ast.literal_eval(match.group(1))  # 直接返回列表
        except SyntaxError:
            _logger.error(f"can't trans to list: {match.group(1)}")
            return []

    @retry_times(retries=5)
    @valid_with(check_valid)
    def invoke_validate_retry(self, messages: list) -> str:
        return self.analyzer.llm.invoke(messages)

    def accept(self):
        self.after_task_prompt()
        _background_messages_copy = copy.deepcopy(self.analyzer.global_history.background_history)
        _background_messages_copy.extend(self.analyzer.global_history.after_task_history)
        _after_select_lines_prompt = [{"role": "user", "content": AFTER_SELECT_LINES_PROMPT}]
        _background_messages_copy.extend(_after_select_lines_prompt)
        valid, response = self.invoke_validate_retry(_background_messages_copy)
        if valid:
            _after_select_lines_prompt.append({"role": "assistant", "content": response})
            self.analyzer.global_history.after_roughly_line_history = _after_select_lines_prompt
            attention_lines = self.get_attention_lines(response)
            self.analyzer.important_after_lines = attention_lines

        self.analyzer.prompt_state = InsertNodeState(self.analyzer)
        return



class InsertNodeState(PromptState):
    def init_history(self):
        element_id = self.analyzer.current_action_node.get("id")
        background_history_copy = copy.deepcopy(self.analyzer.global_history.background_history)
        background_history_copy.extend(self.analyzer.global_history.after_task_history)
        background_history_copy.extend(self.analyzer.global_history.after_roughly_line_history)
        self.analyzer.global_history.element_histories[element_id] = ElementHistory(element_id=element_id,
                                                                                     history=background_history_copy)

    def accept(self):
        if len(self.analyzer.element_stack) > 0:
            self.analyzer.insert_node_analysis()
            self.analyzer.last_state = self.__class__
        else:
            if len(self.analyzer.insert_nodes) > 0:
                insert_node = self.analyzer.insert_nodes.pop(0)
                self.analyzer.current_action_node = insert_node
                self.analyzer.element_stack.append(insert_node)
                self.init_history()
            else:
                self.analyzer.prompt_state = MoveNodeState(self.analyzer)


class MoveNodeState(PromptState):
    def init_history(self):
        element_id = self.analyzer.current_action_node.get("id")
        background_history_copy = copy.deepcopy(self.analyzer.global_history.background_history)
        background_history_copy.extend(self.analyzer.global_history.after_task_history)
        background_history_copy.extend(self.analyzer.global_history.after_roughly_line_history)
        self.analyzer.global_history.element_histories[element_id] = ElementHistory(element_id=element_id,
                                                                                     history=background_history_copy)

    def accept(self):
        if len(self.analyzer.element_stack) > 0:
            self.analyzer.move_node_analysis()
            self.analyzer.last_state = self.__class__
        else:
            if len(self.analyzer.move_parent_nodes) > 0:
                move_parent_node = self.analyzer.move_parent_nodes.pop(0)
                self.analyzer.current_action_node = move_parent_node
                self.analyzer.element_stack.append(move_parent_node)
                self.init_history()
            else:
                self.analyzer.prompt_state = ExitState(self.analyzer)


class InsertElementState(PromptState):
    def accept(self):
        _element = self.analyzer.current_element
        start_line, end_line = _element.get("startLine"), _element.get("endLine")
        if not any(start_line - 1 <= num <= end_line + 1
                   for num in self.analyzer.important_lines):
            self.analyzer.prompt_state = InsertNodeState(self.analyzer)
            return

        _element_prompt = AFTER_TREE_ELEMENT_PROMPT.format(element=_element.get("value"),
                                                           elementType=_element.get("type"),
                                                           line=_element.get("startLine"))

        _element_history = self.analyzer.get_current_element_history()

        _element_his_copy = copy.deepcopy(_element_history)
        _element_his_copy.add_user_message_to_history(_element_prompt)
        _round_prompt = _element_his_copy.history
        valid, response = self.analyzer.invoke_classify_retry(_round_prompt)
        if valid:
            _element_history.add_user_message_to_round(_element_prompt)
            _element_history.add_assistant_message_to_round(response)
            classified_res = self.analyzer.check_classified_num_response(response)
            if classified_res == 1:
                self.analyzer.push(_element)
                self.analyzer.considered_inserts.setdefault(self.analyzer.current_action_node.get("id"), []).append(_element.get("id"))
            elif classified_res == 2:
                self.analyzer.push(_element)
        else:
            _logger.error(f"Invalid response: {response} After retry {self.analyzer.retries} times")
        self.analyzer.prompt_state = InsertNodeState(self.analyzer)
        return


class InsertNameState(PromptState):
    def accept(self):
        _element = self.analyzer.current_element
        _element_prompt = AFTER_TREE_NAME_PROMPT.format(element=_element.get("value"),
                                                        line=_element.get("startLine"))

        _element_history = self.analyzer.get_current_element_history()

        _element_his_copy = copy.deepcopy(_element_history)
        _element_his_copy.add_user_message_to_history(_element_prompt)
        _round_prompt = _element_his_copy.history
        valid, response = self.analyzer.invoke_validate_retry(_round_prompt)
        if valid:
            _element_history.add_user_message_to_round(_element_prompt)
            _element_history.add_assistant_message_to_round(response)
            if self.analyzer.check_true_response(response):
                self.analyzer.considered_inserts.setdefault(self.analyzer.current_action_node.get("id"), []).append(_element.get("id"))
        self.analyzer.prompt_state = RegEXState(self.analyzer)
        return


class MoveElementState(PromptState):
    def accept(self):
        _element = self.analyzer.current_element
        start_line, end_line = _element.get("startLine"), _element.get("endLine")
        if not any(start_line - 1 <= num <= end_line + 1
                   for num in self.analyzer.important_lines):
            self.analyzer.prompt_state = MoveNodeState(self.analyzer)
            return
        _element_prompt = AFTER_TREE_ELEMENT_PROMPT.format(element=_element.get("value"),
                                                           elementType=_element.get("type"),
                                                           line=_element.get("startLine"))

        _element_history = self.analyzer.get_current_element_history()

        _element_his_copy = copy.deepcopy(_element_history)
        _element_his_copy.add_user_message_to_history(_element_prompt)
        _round_prompt = _element_his_copy.history
        valid, response = self.analyzer.invoke_classify_retry(_round_prompt)
        if valid:
            _element_history.add_user_message_to_round(_element_prompt)
            _element_history.add_assistant_message_to_round(response)
            classified_res = self.analyzer.check_classified_num_response(response)
            if classified_res == 1:
                self.analyzer.push(_element)
                self.analyzer.considered_moves.setdefault(self.analyzer.current_action_node.get("id"), []).append(_element.get("id"))
            elif classified_res == 2:
                self.analyzer.push(_element)
        else:
            _logger.error(f"Invalid response: {response} After retry {self.analyzer.retries} times")
        self.analyzer.prompt_state = MoveNodeState(self.analyzer)
        return


class MoveNameState(PromptState):
    def accept(self):
        _element = self.analyzer.current_element
        _element_prompt = AFTER_TREE_NAME_PROMPT.format(element=_element.get("value"),
                                                        line=_element.get("startLine"))

        _element_history = self.analyzer.get_current_element_history()

        _element_his_copy = copy.deepcopy(_element_history)
        _element_his_copy.add_user_message_to_history(_element_prompt)
        _round_prompt = _element_his_copy.history
        valid, response = self.analyzer.invoke_validate_retry(_round_prompt)
        if valid:
            _element_history.add_user_message_to_round(_element_prompt)
            _element_history.add_assistant_message_to_round(response)
            if self.analyzer.check_true_response(response):
                self.analyzer.considered_moves.setdefault(self.analyzer.current_action_node.get("id"), []).append(_element.get("id"))
        self.analyzer.prompt_state = RegEXState(self.analyzer)
        return

