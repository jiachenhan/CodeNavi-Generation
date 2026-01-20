"""
简单的公共状态

包含InitialState和ExitState等简单状态
"""
from app.refine.states.base_state import PromptState


class InitialState(PromptState):
    """初始状态 - 状态机启动入口"""

    def accept(self):
        """转换到第一个实际工作状态"""
        from app.refine.states.analyze_dsl_state import AnalyzeDSLState
        self.refiner.prompt_state = AnalyzeDSLState(self.refiner)


class ExitState(PromptState):
    """退出状态 - 状态机终止"""

    def accept(self):
        """什么都不做，状态机在此终止"""
        pass
