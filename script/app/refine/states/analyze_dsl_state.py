"""
AnalyzeDSLState - DSL分析状态

Step1: 分析DSL代码，理解其语义和可能的问题
"""
import re
from typing import Optional

from app.refine.states.base_state import PromptState
from app.refine.data_structures import RefineStep
from app.refine.prompts import ANALYZE_DSL_PROMPT
from utils.common import retry_times, valid_with
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


# ============================================================================
# 辅助类 - DSL分析结果提取器
# ============================================================================

class DSLAnalysisExtractor:
    """从LLM响应中提取DSL分析结果"""

    PATTERN = re.compile(
        r'\[DSL_ANALYSIS\](.*?)\[/DSL_ANALYSIS\]',
        re.DOTALL | re.IGNORECASE
    )

    @classmethod
    def extract(cls, response: str) -> Optional[str]:
        """
        提取DSL分析结果

        Args:
            response: LLM响应文本

        Returns:
            提取的分析结果，如果未找到则返回None
        """
        match = cls.PATTERN.search(response)
        if match:
            return match.group(1).strip()
        return None

    @classmethod
    def check_valid(cls, response: str) -> bool:
        """检查响应是否包含有效的分析结果块"""
        return bool(cls.PATTERN.search(response))


# ============================================================================
# 主State类
# ============================================================================

class AnalyzeDSLState(PromptState):
    """Step1: 分析DSL状态"""

    def check_valid(self, response: str) -> bool:
        """验证响应格式"""
        return DSLAnalysisExtractor.check_valid(response)

    @retry_times(retries=5)
    @valid_with(check_valid)
    def invoke_validate_retry(self, messages: list) -> str:
        """调用LLM并验证响应格式，失败时自动重试"""
        return self.refiner.llm.invoke(messages)

    def accept(self):
        """执行DSL分析"""
        # Step1: 获取累积历史（此时还没有历史，所以是空列表）
        messages = self.refiner.context.get_accumulated_history(
            up_to_step=RefineStep.ANALYZE_DSL
        )

        prompt = ANALYZE_DSL_PROMPT.format(
            dsl_code=self.refiner.input_data.dsl_code,
            buggy_code=self.refiner.input_data.buggy_code,
            fixed_code=self.refiner.input_data.fixed_code,
            root_cause=self.refiner.input_data.root_cause
        )

        # 添加当前步骤的用户消息
        self.add_user_message(RefineStep.ANALYZE_DSL, prompt, messages)

        try:
            success, response = self.invoke_validate_retry(messages)
            if not success:
                _logger.error("Failed to get valid response after retries")
                from app.refine.states.common_states import ExitState
                self.refiner.prompt_state = ExitState(self.refiner)
                return

            self.add_assistant_message(RefineStep.ANALYZE_DSL, response, messages)
            analysis_result = DSLAnalysisExtractor.extract(response)

            if analysis_result:
                self.refiner.context.dsl_analysis_result = analysis_result
                from app.refine.states.analyze_fp_state import AnalyzeFPState
                self.refiner.prompt_state = AnalyzeFPState(self.refiner)
            else:
                _logger.error("Failed to extract DSL analysis result")
                from app.refine.states.common_states import ExitState
                self.refiner.prompt_state = ExitState(self.refiner)
        except Exception as e:
            _logger.error(f"Error in AnalyzeDSLState: {e}")
            from app.refine.states.common_states import ExitState
            self.refiner.prompt_state = ExitState(self.refiner)
