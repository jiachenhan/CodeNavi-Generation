import copy
from dataclasses import dataclass, field
from itertools import chain
from typing import TypedDict


class Message(TypedDict):
    role: str
    content: str


MessageList = list[Message]


@dataclass
class GlobalHistories:
    background_history: MessageList = field(default_factory=list)
    task_history: MessageList = field(default_factory=list)
    attention_line_history: MessageList = field(default_factory=list)
    identify_elements_history: MessageList = field(default_factory=list)
