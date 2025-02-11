import copy

from app.basic_modification_analysis import background_analysis
from app.history import ElementHistory
from app.prompts import NORMAL_ELEMENT_PROMPT, NAME_ELEMENT_PROMPT, STRUCTURE_ELEMENT_PROMPT, TASK_DESCRIPTION_PROMPT, \
    AFTER_TREE_TASK_PROMPT, AFTER_TREE_ELEMENT_PROMPT, AFTER_TREE_NAME_PROMPT
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)

STRUCTURE_RELATED_AST_NODE_TYPES = ["MoBlock", "MoDoStatement", "MoEnhancedForStatement", "MoForStatement",
                                    "MoIfStatement", "MoSwitchStatement", "MoSynchronizedStatement",
                                    "MoWhileStatement", "MoTryStatement", "MoTypeDeclarationStatement"]

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
    def init_history(self):
        for element in self.analyzer.element_stack:
            element_id = element.get("id")
            background_history_copy = copy.deepcopy(self.analyzer.global_history.background_history)
            background_history_copy.extend(self.analyzer.global_history.task_history)
            self.analyzer.global_history.element_histories[element_id] = ElementHistory(element_id=element_id,
                                                                                        history=background_history_copy)

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
        self.init_history()
        self.analyzer.prompt_state = ElementState(self.analyzer)


class ElementState(PromptState):
    def accept(self):
        if len(self.analyzer.element_stack) > 0:
            self.analyzer.element_analysis()
        else:
            self.analyzer.prompt_state = InsertNodeState(self.analyzer)

class NormalElementState(PromptState):
    def accept(self):
        _element = self.analyzer.current_element
        _element_prompt = NORMAL_ELEMENT_PROMPT.format(line=_element.get("startLine"),
                                                       element=_element.get("value"),
                                                       elementType=_element.get("type"))

        for _ in range(self.analyzer.retries):
            _element_history = self.analyzer.get_current_element_history()

            _element_his_copy = copy.deepcopy(_element_history)
            _element_his_copy.add_user_message_to_history(_element_prompt)
            _round_prompt = _element_his_copy.history
            response = self.analyzer.llm.invoke(_round_prompt)

            if self.analyzer.check_valid_response(response):
                _element_history.add_user_message_to_round(_element_prompt)
                _element_history.add_assistant_message_to_round(response)
                if self.analyzer.check_true_response(response):
                    self.analyzer.push(_element)
                    self.analyzer.prompt_state = StructureState(self.analyzer)
                else:
                    self.analyzer.prompt_state = ElementState(self.analyzer)
                return


class NameState(PromptState):
    def accept(self):
        _element = self.analyzer.current_element
        _element_prompt = NAME_ELEMENT_PROMPT.format(line=_element.get("startLine"),
                                                       element=_element.get("value"))

        for _ in range(self.analyzer.retries):
            _element_history = self.analyzer.get_current_element_history()

            _element_his_copy = copy.deepcopy(_element_history)
            _element_his_copy.add_user_message_to_history(_element_prompt)
            _round_prompt = _element_his_copy.history
            response = self.analyzer.llm.invoke(_round_prompt)

            if self.analyzer.check_valid_response(response):
                _element_history.add_user_message_to_round(_element_prompt)
                _element_history.add_assistant_message_to_round(response)
                if self.analyzer.check_true_response(response):
                    self.analyzer.considered_attrs["Name"].append(_element.get("id"))
                    self.analyzer.considered_elements.add(_element.get("id"))
                self.analyzer.current_element = None
                self.analyzer.prompt_state = ElementState(self.analyzer)
                return

class StructureState(PromptState):
    def accept(self):
        _element = self.analyzer.current_element
        _element_type = _element.get("type")
        if _element_type not in STRUCTURE_RELATED_AST_NODE_TYPES:
            self.analyzer.considered_elements.add(_element.get("id"))
            self.analyzer.current_element = None
            self.analyzer.prompt_state = ElementState(self.analyzer)
            return

        _element_prompt = STRUCTURE_ELEMENT_PROMPT.format(element=_element.get("value"), elementType=_element_type)

        for _ in range(self.analyzer.retries):
            _element_history = self.analyzer.get_current_element_history()

            _element_his_copy = copy.deepcopy(_element_history)
            _element_his_copy.add_user_message_to_history(_element_prompt)
            _round_prompt = _element_his_copy.history
            response = self.analyzer.llm.invoke(_round_prompt)

            if self.analyzer.check_valid_response(response):
                _element_history.add_user_message_to_structure_round(_element_prompt)
                _element_history.add_assistant_message_to_structure_round(response)
                if self.analyzer.check_true_response(response):
                    self.analyzer.considered_elements.add(_element.get("id"))
                self.analyzer.current_element = None
                self.analyzer.prompt_state = ElementState(self.analyzer)
                return


class InsertNodeState(PromptState):
    def init_history(self):
        element_id = self.analyzer.current_action_node.get("id")
        background_history_copy = copy.deepcopy(self.analyzer.global_history.background_history)
        background_history_copy.extend(self.analyzer.global_history.after_task_history)
        self.analyzer.global_history.after_tree_history[element_id] = ElementHistory(element_id=element_id,
                                                                                     history=background_history_copy)

    def after_task_prompt(self):
        _background_messages_copy = copy.deepcopy(self.analyzer.global_history.background_history)
        task_prompt = [{"role": "user", "content": AFTER_TREE_TASK_PROMPT}]
        _background_messages_copy.extend(task_prompt)
        _after_task_response = self.analyzer.llm.invoke(_background_messages_copy)
        task_prompt.append({"role": "assistant", "content": _after_task_response})
        self.analyzer.global_history.after_task_history = task_prompt

    def accept(self):
        if len(self.analyzer.element_stack) > 0:
            self.analyzer.insert_node_analysis()
        else:
            if len(self.analyzer.insert_nodes) > 0:
                insert_node = self.analyzer.insert_nodes.pop(0)
                self.analyzer.current_action_node = insert_node
                self.analyzer.element_stack.append(insert_node)
                self.after_task_prompt()
                self.init_history()
            else:
                self.analyzer.prompt_state = MoveNodeState(self.analyzer)


class MoveNodeState(PromptState):
    def init_history(self):
        element_id = self.analyzer.current_action_node.get("id")
        background_history_copy = copy.deepcopy(self.analyzer.global_history.background_history)
        background_history_copy.extend(self.analyzer.global_history.after_task_history)
        self.analyzer.global_history.after_tree_history[element_id] = ElementHistory(element_id=element_id,
                                                                                     history=background_history_copy)

    def accept(self):
        if len(self.analyzer.element_stack) > 0:
            self.analyzer.move_node_analysis()
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
        _element_prompt = AFTER_TREE_ELEMENT_PROMPT.format(element=_element.get("value"),
                                                           elementType=_element.get("type"))

        for _ in range(self.analyzer.retries):
            _element_history = self.analyzer.get_action_current_element_history()

            _element_his_copy = copy.deepcopy(_element_history)
            _element_his_copy.add_user_message_to_history(_element_prompt)
            _round_prompt = _element_his_copy.history
            response = self.analyzer.llm.invoke(_round_prompt)

            if self.analyzer.check_valid_response(response):
                _element_history.add_user_message_to_round(_element_prompt)
                _element_history.add_assistant_message_to_round(response)
                if self.analyzer.check_true_response(response):
                    self.analyzer.considered_inserts.setdefault(self.analyzer.current_action_node.get("id"), []).append(_element.get("id"))
                    self.analyzer.push_action(_element)
                self.analyzer.prompt_state = InsertNodeState(self.analyzer)
                return


class InsertNameState(PromptState):
    def accept(self):
        _element = self.analyzer.current_element
        _element_prompt = AFTER_TREE_NAME_PROMPT.format(element=_element.get("value"))

        for _ in range(self.analyzer.retries):
            _element_history = self.analyzer.get_action_current_element_history()

            _element_his_copy = copy.deepcopy(_element_history)
            _element_his_copy.add_user_message_to_history(_element_prompt)
            _round_prompt = _element_his_copy.history
            response = self.analyzer.llm.invoke(_round_prompt)

            if self.analyzer.check_valid_response(response):
                _element_history.add_user_message_to_round(_element_prompt)
                _element_history.add_assistant_message_to_round(response)
                if self.analyzer.check_true_response(response):
                    self.analyzer.considered_inserts.setdefault(self.analyzer.current_action_node.get("id"), []).append(_element.get("id"))
                self.analyzer.current_element = None
                self.analyzer.prompt_state = InsertNodeState(self.analyzer)
                return

class MoveElementState(PromptState):
    def accept(self):
        _element = self.analyzer.current_element
        _element_prompt = AFTER_TREE_ELEMENT_PROMPT.format(element=_element.get("value"),
                                                           elementType=_element.get("type"))

        for _ in range(self.analyzer.retries):
            _element_history = self.analyzer.get_action_current_element_history()

            _element_his_copy = copy.deepcopy(_element_history)
            _element_his_copy.add_user_message_to_history(_element_prompt)
            _round_prompt = _element_his_copy.history
            response = self.analyzer.llm.invoke(_round_prompt)

            if self.analyzer.check_valid_response(response):
                _element_history.add_user_message_to_round(_element_prompt)
                _element_history.add_assistant_message_to_round(response)
                if self.analyzer.check_true_response(response):
                    self.analyzer.considered_moves.setdefault(self.analyzer.current_action_node.get("id"), []).append(_element.get("id"))
                    self.analyzer.push_action(_element)
                self.analyzer.prompt_state = MoveNodeState(self.analyzer)
                return

class MoveNameState(PromptState):
    def accept(self):
        _element = self.analyzer.current_element
        _element_prompt = AFTER_TREE_NAME_PROMPT.format(element=_element.get("value"))

        for _ in range(self.analyzer.retries):
            _element_history = self.analyzer.get_action_current_element_history()

            _element_his_copy = copy.deepcopy(_element_history)
            _element_his_copy.add_user_message_to_history(_element_prompt)
            _round_prompt = _element_his_copy.history
            response = self.analyzer.llm.invoke(_round_prompt)

            if self.analyzer.check_valid_response(response):
                _element_history.add_user_message_to_round(_element_prompt)
                _element_history.add_assistant_message_to_round(response)
                if self.analyzer.check_true_response(response):
                    self.analyzer.considered_moves.setdefault(self.analyzer.current_action_node.get("id"), []).append(_element.get("id"))
                self.analyzer.current_element = None
                self.analyzer.prompt_state = MoveNodeState(self.analyzer)
                return

# class AttrState(PromptState):
#     def accept(self):
#         pass