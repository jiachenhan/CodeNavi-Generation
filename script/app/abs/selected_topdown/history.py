import copy
from dataclasses import dataclass, field
from itertools import chain
from typing import TypedDict


class Message(TypedDict):
    role: str
    content: str

MessageList = list[Message]

@dataclass
class ElementHistory:
    element_id: int
    history: MessageList
    round: MessageList = field(default_factory=list)
    considered_sub_elements: list[int] = field(default_factory=list)

    def get_history(self) -> MessageList:
        return copy.deepcopy(self.history)

    def get_round_history(self) -> MessageList:
        return list(chain(self.history, self.round))

    def add_message(self, role: str, content: str) -> None:
        self.round.append({"role": role, "content": content})

    def add_user_message(self, content: str) -> None:
        self.add_message("user", content)

    def add_assistant_message(self, content: str) -> None:
        self.add_message("assistant", content)


@dataclass
class GlobalHistories:
    background_history: MessageList = field(default_factory=list)
    task_history: MessageList = field(default_factory=list)
    select_stmts_history: MessageList = field(default_factory=list)
    element_histories: dict[int, ElementHistory] = field(default_factory=dict)
    after_task_history: MessageList = field(default_factory=list)
    after_element_histories: dict[int, ElementHistory] = field(default_factory=dict)

    def get_background_history(self) -> MessageList:
        return copy.deepcopy(self.background_history)

    def merge_before_task_history(self) -> MessageList:
        return list(chain(self.background_history, self.task_history))

    def merge_stmts_history(self) -> MessageList:
        return list(chain(self.background_history, self.task_history, self.select_stmts_history))

    def merge_after_task_history(self) -> MessageList:
        return list(chain(self.background_history, self.after_task_history))