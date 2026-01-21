"""
ValidateConstraintState - 约束验证状态

Step3.5: 验证提取的约束，如果有错误则请求LLM修复
"""
import re
from typing import List
from dataclasses import dataclass
from enum import Enum

from app.refine.states.base_state import PromptState
from app.refine.states.extract_constraint_state import ConstraintResponseParser
from app.refine.data_structures import ExtraConstraint, RefineStep
from app.refine.prompts import VALIDATE_CONSTRAINT_PROMPT
from app.refine.parser.validators import ConstraintValidator
from utils.common import retry_times, valid_with
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


# ============================================================================
# 约束验证状态枚举
# ============================================================================

class ConstraintValidationStatus(Enum):
    """约束验证状态"""
    VALID = "valid"                # 合法 - 直接保留
    FIXABLE = "fixable"            # 不合法但可修复 - 发送给LLM修复
    INVALID = "invalid"            # 无效不可修复 - 直接丢弃


# ============================================================================
# 辅助类 - 约束验证结果
# ============================================================================

@dataclass
class ConstraintValidationItem:
    """单个约束的验证结果"""
    index: int                                      # 约束在列表中的索引（从0开始）
    constraint: ExtraConstraint                     # 约束对象
    status: ConstraintValidationStatus              # 验证状态
    error_message: str = ""                         # 主要错误消息（合并所有errors）
    fix_suggestion: str = ""                        # 修复建议（合并所有suggestions）

    @property
    def constraint_number(self) -> int:
        """约束编号（从1开始，用于显示）"""
        return self.index + 1

    @property
    def is_valid(self) -> bool:
        """是否合法"""
        return self.status == ConstraintValidationStatus.VALID

    @property
    def is_fixable(self) -> bool:
        """是否可修复"""
        return self.status == ConstraintValidationStatus.FIXABLE

    @property
    def should_discard(self) -> bool:
        """是否应该丢弃"""
        return self.status == ConstraintValidationStatus.INVALID


# ============================================================================
# 辅助类 - 约束验证器Helper
# ============================================================================

class ConstraintValidationHelper:
    """约束验证辅助类 - 封装验证逻辑"""

    def __init__(self, validator: ConstraintValidator):
        self.validator = validator

    def _determine_status(self, validation_result) -> ConstraintValidationStatus:
        """
        根据验证结果决定约束状态

        Args:
            validation_result: 验证器返回的ValidationResult

        Returns:
            约束状态 (VALID/FIXABLE/INVALID)
        """
        if validation_result.is_valid:
            return ConstraintValidationStatus.VALID

        # 检查是否有任何错误标记为应该丢弃约束
        for error in validation_result.errors:
            if error.should_discard_constraint:
                return ConstraintValidationStatus.INVALID

        # 默认为可修复
        return ConstraintValidationStatus.FIXABLE

    def validate_all(
        self,
        constraints: List[ExtraConstraint],
        original_dsl: str
    ) -> List[ConstraintValidationItem]:
        """
        验证所有约束，返回结构化的验证结果

        Args:
            constraints: 约束列表
            original_dsl: 原始DSL代码

        Returns:
            验证结果列表，每个元素包含约束及其验证信息
        """
        results = []

        for idx, constraint in enumerate(constraints):
            validation_result = self.validator.validate(constraint, original_dsl)

            # 决定约束状态
            status = self._determine_status(validation_result)

            # 合并所有错误消息和建议
            error_message = "; ".join(error.message for error in validation_result.errors)
            suggestions = [error.suggestion for error in validation_result.errors if error.suggestion]
            fix_suggestion = "\n".join(suggestions) if suggestions else ""

            item = ConstraintValidationItem(
                index=idx,
                constraint=constraint,
                status=status,
                error_message=error_message,
                fix_suggestion=fix_suggestion
            )
            results.append(item)

            # 记录丢弃的约束
            if status == ConstraintValidationStatus.INVALID:
                _logger.warning(
                    f"Constraint {idx + 1} will be discarded (unfixable): {error_message}"
                )

        return results


# ============================================================================
# 辅助类 - 约束格式化器（用于生成LLM prompt）
# ============================================================================

class ConstraintFormatter:
    """格式化约束用于LLM修复"""

    @staticmethod
    def format_fixable_constraints(validation_items: List[ConstraintValidationItem]) -> str:
        """
        只格式化可修复的约束，包含错误信息和修复建议

        注意：不包含应该丢弃的约束(INVALID状态)

        Args:
            validation_items: 验证结果列表

        Returns:
            格式化的字符串，只包含可修复的约束
        """
        lines = []
        fixable_items = [item for item in validation_items if item.is_fixable]

        if not fixable_items:
            return ""

        lines.append(f"Found {len(fixable_items)} fixable constraint(s):\n")

        for item in fixable_items:
            c = item.constraint
            lines.append(f"Constraint {item.constraint_number}:")
            lines.append(f"- Type: {c.constraint_type.value}")
            lines.append(f"- Path: {c.constraint_path}")
            lines.append(f"- Operator: {c.operator}")
            lines.append(f"- Value: {c.value}")
            lines.append(f"- Is Negative: {'yes' if c.is_negative else 'no'}")
            lines.append("")
            lines.append(f"ERROR: {item.error_message}")
            if item.fix_suggestion:
                lines.append(f"HOW TO FIX: {item.fix_suggestion}")
            lines.append("")

        return "\n".join(lines)


# ============================================================================
# 主State类
# ============================================================================

class ValidateConstraintState(PromptState):
    """Step3.5: 验证和修复约束"""

    CONSTRAINTS_PATTERN = re.compile(
        r'\[CONSTRAINTS\](.*?)\[/CONSTRAINTS\]',
        re.DOTALL | re.IGNORECASE
    )

    MAX_RETRIES = 3

    def check_valid(self, response: str) -> bool:
        """验证响应格式"""
        return bool(self.CONSTRAINTS_PATTERN.search(response))

    @retry_times(retries=5)
    @valid_with(check_valid)
    def invoke_validate_retry(self, messages: list) -> str:
        """调用LLM并验证响应格式，失败时自动重试"""
        return self.refiner.llm.invoke(messages)

    def parse_constraints(self, response: str, source_type: str) -> List[ExtraConstraint]:
        """复用ExtractConstraintState的解析逻辑"""
        parser = ConstraintResponseParser(source_type)
        return parser.parse(response)

    def accept(self):
        """执行约束验证和修复"""
        _logger.info("Validating extracted constraints...")

        constraints = self.refiner.context.extracted_constraints
        original_dsl = self.refiner.input_data.dsl_code

        # 初始化验证器
        validator = ConstraintValidator()
        validation_helper = ConstraintValidationHelper(validator)

        # 验证所有约束
        validation_results = validation_helper.validate_all(constraints, original_dsl)

        # 分类约束
        valid_items = [item for item in validation_results if item.is_valid]
        fixable_items = [item for item in validation_results if item.is_fixable]
        invalid_items = [item for item in validation_results if item.should_discard]

        _logger.info(
            f"Validation complete: {len(valid_items)} valid, "
            f"{len(fixable_items)} fixable, {len(invalid_items)} discarded"
        )

        # 如果所有约束都合法，直接进入下一步
        if not fixable_items and not invalid_items:
            _logger.info("All constraints are valid")
            self.refiner.context.extracted_constraints = constraints
            from app.refine.states.construct_dsl_state import ConstructDSLState
            self.refiner.prompt_state = ConstructDSLState(self.refiner)
            return

        # 如果没有可修复的约束（只有无效的），保留有效约束继续
        if not fixable_items:
            _logger.warning(f"No fixable constraints, keeping {len(valid_items)} valid constraints")
            valid_constraints = [item.constraint for item in valid_items]
            self.refiner.context.extracted_constraints = valid_constraints
            from app.refine.states.construct_dsl_state import ConstructDSLState
            self.refiner.prompt_state = ConstructDSLState(self.refiner)
            return

        # 有可修复约束，进入修复流程
        _logger.info(f"Found {len(fixable_items)} fixable constraints, requesting LLM to fix...")

        # 尝试修复（最多MAX_RETRIES次）
        for attempt in range(1, self.MAX_RETRIES + 1):
            _logger.info(f"Fix attempt {attempt}/{self.MAX_RETRIES}")

            # 构建修复prompt - 只发送可修复的约束
            messages = self.refiner.context.get_accumulated_history(
                up_to_step=RefineStep.VALIDATE_CONSTRAINT
            )

            fixable_constraints_info = ConstraintFormatter.format_fixable_constraints(validation_results)

            prompt = VALIDATE_CONSTRAINT_PROMPT.format(
                original_dsl=original_dsl,
                fixable_constraints=fixable_constraints_info
            )

            self.add_user_message(RefineStep.VALIDATE_CONSTRAINT, prompt, messages)

            try:
                success, response = self.invoke_validate_retry(messages)
                if not success:
                    _logger.error("Failed to get valid response after retries")
                    # 保留有效约束继续
                    valid_constraints = [item.constraint for item in valid_items]
                    self.refiner.context.extracted_constraints = valid_constraints
                    from app.refine.states.construct_dsl_state import ConstructDSLState
                    self.refiner.prompt_state = ConstructDSLState(self.refiner)
                    return

                self.add_assistant_message(RefineStep.VALIDATE_CONSTRAINT, response, messages)

                # 解析修复后的约束
                source_type = constraints[0].source_file if constraints else "buggy"
                fixed_constraints = self.parse_constraints(response, source_type)

                if not fixed_constraints:
                    _logger.warning(f"Attempt {attempt}: No constraints parsed from fix response")
                    continue

                # 将修复后的约束合并回原列表（替换fixable的约束）
                merged_constraints = [item.constraint for item in valid_items]  # 先保留所有有效的
                merged_constraints.extend(fixed_constraints)  # 加上修复的

                # 重新验证合并后的约束
                validation_results = validation_helper.validate_all(merged_constraints, original_dsl)
                valid_items = [item for item in validation_results if item.is_valid]
                fixable_items = [item for item in validation_results if item.is_fixable]
                invalid_items = [item for item in validation_results if item.should_discard]

                if not fixable_items and not invalid_items:
                    _logger.info(f"Attempt {attempt}: All constraints fixed successfully")
                    self.refiner.context.extracted_constraints = merged_constraints
                    from app.refine.states.construct_dsl_state import ConstructDSLState
                    self.refiner.prompt_state = ConstructDSLState(self.refiner)
                    return
                else:
                    _logger.warning(
                        f"Attempt {attempt}: Still have {len(fixable_items)} fixable, "
                        f"{len(invalid_items)} invalid constraints"
                    )

            except Exception as e:
                _logger.error(f"Attempt {attempt}: Error during constraint fix: {e}")
                continue

        # 所有重试都失败，保留有效约束继续
        _logger.error(f"Failed to fix constraints after {self.MAX_RETRIES} attempts")
        valid_constraints = [item.constraint for item in valid_items]
        self.refiner.context.extracted_constraints = valid_constraints
        from app.refine.states.construct_dsl_state import ConstructDSLState
        self.refiner.prompt_state = ConstructDSLState(self.refiner)
