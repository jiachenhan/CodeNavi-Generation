import json
import re
from pathlib import Path

from app.abs.selected_topdown.history import GlobalHistories
from app.abs.selected_topdown.prompt_state import PromptState, InitialState, ExitState
from app.communication import PatternInput
from interface.llm.llm_api import LLMAPI
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)

class Analyzer:
    def __init__(self,
                 _llm: LLMAPI,
                 _pattern_input: PatternInput,
                 _retries: int=5):
        self.llm = _llm
        self.pattern_input = _pattern_input
        self.retries = _retries

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
        pass

    @staticmethod
    def get_considered_nodes(response: str) -> list[int]:
        pass

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
        for _id, _element_history in self.global_history.after_element_histories.items():
            after_histories[_id] = {
                "history": _element_history.history,
                "round": _element_history.element_round,
                "structure_round": _element_history.structure_round
            }
        histories["afters"] = after_histories

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

    def push(self, sub_trees: list[dict], in_before_tree: bool) -> None:
        pass

    def analysis(self):
        while not isinstance(self.prompt_state, ExitState):
            self.prompt_state.accept()
