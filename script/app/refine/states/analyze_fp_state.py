"""
AnalyzeFPState - FP分析状态

Step2: 分析False Positive（误报），确定refinement的Scenario
"""
import re
from typing import Optional

from app.refine.states.base_state import PromptState
from app.refine.data_structures import RefineStep
from app.refine.prompts import ANALYZE_FP_PROMPT
from utils.common import retry_times, valid_with
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


# ============================================================================
# 辅助类 - FP分析结果提取器
# ============================================================================

class FPAnalysisExtractor:
    """从LLM响应中提取FP分析结果"""

    ANALYSIS_PATTERN = re.compile(
        r'\[FP_ANALYSIS\](.*?)\[/FP_ANALYSIS\]',
        re.DOTALL | re.IGNORECASE
    )

    SCENARIO_PATTERN = re.compile(
        r'Scenario[:\s]+(\d+)',
        re.IGNORECASE
    )

    @classmethod
    def extract_analysis(cls, response: str) -> Optional[str]:
        """提取FP分析结果"""
        match = cls.ANALYSIS_PATTERN.search(response)
        if match:
            return match.group(1).strip()
        return None

    @classmethod
    def extract_scenario(cls, response: str) -> Optional[int]:
        """提取Scenario编号"""
        match = cls.SCENARIO_PATTERN.search(response)
        if match:
            try:
                scenario = int(match.group(1))
                if 1 <= scenario <= 3:
                    return scenario
                else:
                    _logger.warning(f"Scenario {scenario} out of range (1-3), defaulting to 2")
                    return 2
            except ValueError:
                _logger.warning(f"Invalid scenario number: {match.group(1)}")
                return 2
        return None

    @classmethod
    def check_valid(cls, response: str) -> bool:
        """检查响应是否包含有效的分析结果块"""
        return bool(cls.ANALYSIS_PATTERN.search(response))


# ============================================================================
# 主State类
# ============================================================================

class AnalyzeFPState(PromptState):
    """Step2: 分析FP状态"""

    def check_valid(self, response: str) -> bool:
        """验证响应格式"""
        return FPAnalysisExtractor.check_valid(response)

    @retry_times(retries=5)
    @valid_with(check_valid)
    def invoke_validate_retry(self, messages: list) -> str:
        """调用LLM并验证响应格式，失败时自动重试"""
        return self.refiner.llm.invoke(messages)

    def accept(self):
        """执行FP分析"""
        # Step2: 获取累积历史（包含Step1的对话）
        messages = self.refiner.context.get_accumulated_history(
            up_to_step=RefineStep.ANALYZE_FP
        )

        prompt = ANALYZE_FP_PROMPT.format(
            fp_code=self.refiner.input_data.fp_code
        )

        # 添加当前步骤的用户消息
        self.add_user_message(RefineStep.ANALYZE_FP, prompt, messages)

        try:
            success, response = self.invoke_validate_retry(messages)
            if not success:
                _logger.error("Failed to get valid response after retries")
                from app.refine.states.common_states import ExitState
                self.refiner.prompt_state = ExitState(self.refiner)
                return

            self.add_assistant_message(RefineStep.ANALYZE_FP, response, messages)
            fp_analysis = FPAnalysisExtractor.extract_analysis(response)

            if fp_analysis:
                self.refiner.context.fp_analysis_result = fp_analysis
                # 提取Scenario信息
                scenario = FPAnalysisExtractor.extract_scenario(response)
                if scenario:
                    self.refiner.context.fp_scenario = scenario
                from app.refine.states.extract_constraint_state import ExtractConstraintState
                self.refiner.prompt_state = ExtractConstraintState(self.refiner)
            else:
                _logger.error("Failed to extract FP analysis result")
                from app.refine.states.common_states import ExitState
                self.refiner.prompt_state = ExitState(self.refiner)
        except Exception as e:
            _logger.error(f"Error in AnalyzeFPState: {e}")
            from app.refine.states.common_states import ExitState
            self.refiner.prompt_state = ExitState(self.refiner)
