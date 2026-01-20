"""
状态机基类

定义所有State的公共接口和辅助方法
"""
from abc import ABC, abstractmethod


class PromptState(ABC):
    """状态机基类"""

    def __init__(self, refiner):
        """
        Args:
            refiner: DSLRefiner实例，提供对上下文和配置的访问
        """
        self.refiner = refiner

    @abstractmethod
    def accept(self):
        """
        状态处理方法 - 子类必须实现

        该方法负责：
        1. 执行当前状态的主要逻辑
        2. 设置下一个状态 (self.refiner.prompt_state = NextState(self.refiner))
        """
        raise NotImplementedError

    def add_user_message(self, step, content: str, messages: list):
        """
        添加用户消息到对话历史

        Args:
            step: RefineStep枚举值
            content: 消息内容
            messages: 消息列表（会被修改）
        """
        message = {"role": "user", "content": content}
        messages.append(message)

        # 同时更新上下文中的历史
        self.refiner.context.add_message(step, "user", content)

    def add_assistant_message(self, step, content: str, messages: list):
        """
        添加助手消息到对话历史

        Args:
            step: RefineStep枚举值
            content: 消息内容
            messages: 消息列表（会被修改）
        """
        message = {"role": "assistant", "content": content}
        messages.append(message)

        # 同时更新上下文中的历史
        self.refiner.context.add_message(step, "assistant", content)
