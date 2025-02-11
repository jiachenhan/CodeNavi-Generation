import copy
import json
import os
import re
from pathlib import Path

from app.communication import PatternInput
from app.history import GlobalHistories, ElementHistory
from app.prompt_state import (PromptState, InitialState, ExitState, NameState, NormalElementState,
                              InsertElementState, InsertNameState, MoveNameState, MoveElementState)
from interface.llm.llm_api import LLMAPI
from interface.llm.llm_openai import LLMOpenAI
from utils.config import LoggerConfig, set_config, get_pattern_info_base_path

_logger = LoggerConfig.get_logger(__name__)


class Analyzer:
    def __init__(self,
                 llm: LLMAPI,
                 pattern_input: PatternInput,
                 retries: int=5):
        self.llm = llm
        self.pattern_input = pattern_input
        self.retries = retries

        self.prompt_state: PromptState = InitialState(self)
        self.current_element = None
        self.element_stack = list(reversed(Analyzer.get_top_stmts_from_tree(self.pattern_input.tree)))

        self.global_history = GlobalHistories()
        self.considered_elements = set()
        self.considered_attrs = {"exprType": [], "Name": []}

        self.insert_nodes = [node for node in self.pattern_input.insert_nodes]
        self.move_parent_nodes = [node for node in self.pattern_input.move_parent_nodes]
        # 标识当前正在分析的插入/移动父节点
        self.current_action_node = None
        self.considered_inserts = {}
        self.considered_moves = {}

    @staticmethod
    def get_top_stmts_from_tree(tree: dict) -> list:
        for _ in tree["children"]:
            if _["type"] == "MoBlock":
                return _["children"]

    @staticmethod
    def check_valid_response(response: str) -> bool:
        # 使用正则表达式删除第一个字母之前的所有符号，并保留整个字符串
        cleaned_response = re.sub(r'^[^a-zA-Z]*([a-zA-Z])', r'\1', response)
        is_valid = cleaned_response.lower().startswith(("yes", "no"))
        if not is_valid:
            _logger.error(f"Retry! Invalid response: {response}")
        return is_valid

    @staticmethod
    def check_true_response(response: str) -> bool:
        # 使用正则表达式删除第一个字母之前的所有符号，并保留整个字符串
        cleaned_response = re.sub(r'^[^a-zA-Z]*([a-zA-Z])', r'\1', response)
        return cleaned_response.lower().startswith("yes")

    @staticmethod
    def is_name_element(_element: dict) -> bool:
        return _element.get("type") in ("MoSimpleName", "MoQualifiedName")

    def serialize(self, path: Path):
        histories = {"background": self.global_history.background_history, "task": self.global_history.task_history}

        element_histories = {}
        for _id, _element_history in self.global_history.element_histories.items():
            element_histories[_id] = {
                "history": _element_history.history,
                "round": _element_history.element_round,
                "structure_round": _element_history.structure_round
            }
        histories["elements"] = element_histories

        after_histories = {}
        for _id, _element_history in self.global_history.after_tree_history.items():
            after_histories[_id] = {
                "history": _element_history.history,
                "round": _element_history.element_round,
                "structure_round": _element_history.structure_round
            }
        histories["elements"] = after_histories

        data = {
            "histories": histories,
            "considered_elements": list(self.considered_elements),
            "considered_attrs": self.considered_attrs,
            "insert_elements": self.considered_inserts,
            "move_elements": self.considered_moves
        }

        if not path.exists():
            path.parent.mkdir(parents=True, exist_ok=True)
            _logger.info(f"Create file {path}")

        with open(path, "w") as f:
            json.dump(data, f, indent=4)

    def push(self, sub_tree: dict) -> None:
        if sub_tree.get("leaf"):
            return
        for child in reversed(sub_tree.get("children")):
            element_id = sub_tree.get("id")
            child_id = child.get("id")
            round_history = copy.deepcopy(self.global_history.element_histories.get(element_id).history)
            _round = self.global_history.element_histories.get(element_id).element_round
            round_history.extend(_round)
            self.global_history.element_histories[child_id] = ElementHistory(element_id=child_id, history=round_history)
            self.element_stack.append(child)

    def push_action(self, sub_tree: dict) -> None:
        if sub_tree.get("leaf"):
            return
        for child in reversed(sub_tree.get("children")):
            element_id = sub_tree.get("id")
            child_id = child.get("id")
            round_history = copy.deepcopy(self.global_history.after_tree_history.get(element_id).history)
            _round = self.global_history.after_tree_history.get(element_id).element_round
            round_history.extend(_round)
            self.global_history.after_tree_history[child_id] = ElementHistory(element_id=child_id, history=round_history)
            self.element_stack.append(child)

    def get_current_element_history(self) -> ElementHistory:
        return self.global_history.element_histories.get(self.current_element.get("id"))

    def analysis(self):
        while not isinstance(self.prompt_state, ExitState):
            self.prompt_state.accept()

    def element_analysis(self):
        self.current_element = self.element_stack.pop()
        if Analyzer.is_name_element(self.current_element):
            self.prompt_state = NameState(self)
        else:
            self.prompt_state = NormalElementState(self)

    def get_action_current_element_history(self) -> ElementHistory:
        return self.global_history.after_tree_history.get(self.current_element.get("id"))

    def insert_node_analysis(self):
        self.current_element = self.element_stack.pop()
        if Analyzer.is_name_element(self.current_element):
            self.prompt_state = InsertNameState(self)
        else:
            self.prompt_state = InsertElementState(self)

    def move_node_analysis(self):
        self.current_element = self.element_stack.pop()
        if Analyzer.is_name_element(self.current_element):
            self.prompt_state = MoveNameState(self)
        else:
            self.prompt_state = MoveElementState(self)

if __name__ == "__main__":
    set_config()
    llm = LLMOpenAI(base_url=os.environ.get("OPENAI_BASE_URL"),
                    api_key=os.environ.get("OPENAI_API_KEY"),
                    model_name=os.environ.get("MODEL_NAME"))

    dataset_name = ""
    group_name = ""
    input_path = get_pattern_info_base_path() / "input" / dataset_name / f"{group_name}.json"
    output_path = get_pattern_info_base_path() / "output" / f"{group_name}.json"
    pattern_input = PatternInput.from_file(input_path)
    analyzer = Analyzer(llm, pattern_input)
    analyzer.analysis()
    analyzer.serialize(output_path)
