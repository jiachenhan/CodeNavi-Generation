import copy
from typing import List, Dict


class ElementHistory:
    def __init__(self,
                 element_id: int,
                 history: list):
        self.element_id = element_id
        self.history = history
        self.element_round = list()
        self.structure_round = list()
        self.regex_round = list()

    def get_round_history(self) -> List[Dict]:
        history_copy = copy.deepcopy(self.history)
        history_copy.extend(self.element_round)
        return history_copy

    def add_message_to_history(self, role: str, content: str):
        self.history.append({"role": role, "content": content})

    def add_user_message_to_history(self, content: str):
        self.add_message_to_history(role="user", content=content)

    def add_assistant_message_to_history(self, content: str):
        self.add_message_to_history(role="assistant", content=content)

    def add_message_to_round(self, role: str, content: str):
        self.element_round.append({"role": role, "content": content})

    def add_user_message_to_round(self, content: str):
        self.add_message_to_round(role="user", content=content)

    def add_assistant_message_to_round(self, content: str):
        self.add_message_to_round(role="assistant", content=content)

    def add_message_to_structure_round(self, role: str, content: str):
        self.structure_round.append({"role": role, "content": content})

    def add_user_message_to_structure_round(self, content: str):
        self.add_message_to_structure_round(role="user", content=content)

    def add_assistant_message_to_structure_round(self, content: str):
        self.add_message_to_structure_round(role="assistant", content=content)


class GlobalHistories:
    def __init__(self):
        self.background_history = []
        self.roughly_line_history = []
        self.task_history = []
        self.element_histories: Dict[int, ElementHistory] = {}
        self.after_task_history = []
        self.after_tree_history: Dict[int, ElementHistory] = {}
