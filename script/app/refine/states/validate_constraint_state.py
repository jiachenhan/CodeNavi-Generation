"""
ValidateConstraintState - 约束验证状态

Step3.5: 验证提取的约束，如果有错误则请求LLM修复
"""
import re
import json
from typing import List, Tuple

from app.refine.states.base_state import PromptState
from app.refine.states.extract_constraint_state import ConstraintResponseParser
from app.refine.data_structures import ExtraConstraint, RefineStep
from app.refine.prompts import VALIDATE_CONSTRAINT_PROMPT
from app.refine.parser.validators import ConstraintValidator, ValidationResult, DSLFixSuggester
from utils.common import retry_times, valid_with
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


# ============================================================================
# 辅助类 - 约束验证器Helper
# ============================================================================

class ConstraintValidationHelper:
    """约束验证辅助类 - 封装验证逻辑"""

    def __init__(self, validator: ConstraintValidator):
        self.validator = validator

    def validate_all(
        self,
        constraints: List[ExtraConstraint],
        original_dsl: str
    ) -> Tuple[List[int], List[str], List[str]]:
        """
        验证所有约束，返回无效约束的索引、错误消息和修复建议

        Args:
            constraints: 约束列表
            original_dsl: 原始DSL代码

        Returns:
            (invalid_indices, error_messages, fix_suggestions)
        """
        invalid_indices = []
        error_messages = []
        fix_suggestions = []

        for idx, constraint in enumerate(constraints):
            validation_result = self.validator.validate(constraint, original_dsl)
            if not validation_result.is_valid:
                invalid_indices.append(idx)
                # 收集错误消息
                for error in validation_result.errors:
                    error_messages.append(f"Constraint {idx + 1}: {error.message}")
                    if error.suggestion:
                        fix_suggestions.append(f"Constraint {idx + 1}: {error.suggestion}")

        return invalid_indices, error_messages, fix_suggestions


# ============================================================================
# 辅助类 - 约束JSON序列化器
# ============================================================================

class ConstraintJsonSerializer:
    """将约束列表序列化为JSON格式"""

    @staticmethod
    def serialize(constraints: List[ExtraConstraint]) -> str:
        """
        将约束列表序列化为JSON字符串

        Args:
            constraints: 约束列表

        Returns:
            格式化的JSON字符串
        """
        constraints_data = [
            {
                "path": c.constraint_path,
                "operator": c.operator,
                "value": c.value,
                "type": c.constraint_type.value,
                "original_value": c.original_value
            }
            for c in constraints
        ]
        return json.dumps(constraints_data, indent=2, ensure_ascii=False)


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
        invalid_indices, error_messages, fix_suggestions = validation_helper.validate_all(
            constraints,
            original_dsl
        )

        # 如果所有约束都有效，直接进入下一步
        if not invalid_indices:
            _logger.info("All constraints are valid")
            self.refiner.context.extracted_constraints = constraints
            from app.refine.states.construct_dsl_state import ConstructDSLState
            self.refiner.prompt_state = ConstructDSLState(self.refiner)
            return

        # 有无效约束，进入修复流程
        _logger.warning(f"Found {len(invalid_indices)} invalid constraints, requesting LLM to fix...")

        # 尝试修复（最多MAX_RETRIES次）
        for attempt in range(1, self.MAX_RETRIES + 1):
            _logger.info(f"Fix attempt {attempt}/{self.MAX_RETRIES}")

            # 构建修复prompt
            messages = self.refiner.context.get_accumulated_history(
                up_to_step=RefineStep.VALIDATE_CONSTRAINT
            )

            prompt = VALIDATE_CONSTRAINT_PROMPT.format(
                original_dsl=original_dsl,
                constraints_json=ConstraintJsonSerializer.serialize(constraints),
                validation_errors='\n'.join(error_messages),
                fix_suggestions='\n'.join(fix_suggestions) if fix_suggestions else "No suggestions available"
            )

            self.add_user_message(RefineStep.VALIDATE_CONSTRAINT, prompt, messages)

            try:
                success, response = self.invoke_validate_retry(messages)
                if not success:
                    _logger.error("Failed to get valid response after retries")
                    # 保留有效约束继续
                    valid_constraints = [c for idx, c in enumerate(constraints) if idx not in invalid_indices]
                    self.refiner.context.extracted_constraints = valid_constraints
                    from app.refine.states.construct_dsl_state import ConstructDSLState
                    self.refiner.prompt_state = ConstructDSLState(self.refiner)
                    return

                self.add_assistant_message(RefineStep.VALIDATE_CONSTRAINT, response, messages)

                # 解析修复后的约束
                # 使用第一个约束的source_type作为默认值
                source_type = constraints[0].source_file if constraints else "buggy"
                fixed_constraints = self.parse_constraints(response, source_type)

                if not fixed_constraints:
                    _logger.warning(f"Attempt {attempt}: No constraints parsed from fix response")
                    continue

                # 重新验证修复后的约束
                invalid_indices, error_messages, fix_suggestions = validation_helper.validate_all(
                    fixed_constraints,
                    original_dsl
                )

                if not invalid_indices:
                    _logger.info(f"Attempt {attempt}: All constraints fixed successfully")
                    self.refiner.context.extracted_constraints = fixed_constraints
                    from app.refine.states.construct_dsl_state import ConstructDSLState
                    self.refiner.prompt_state = ConstructDSLState(self.refiner)
                    return
                else:
                    _logger.warning(
                        f"Attempt {attempt}: Still have {len(invalid_indices)} invalid constraints"
                    )
                    constraints = fixed_constraints

            except Exception as e:
                _logger.error(f"Attempt {attempt}: Error during constraint fix: {e}")
                continue

        # 所有重试都失败，保留有效约束继续
        _logger.error(f"Failed to fix constraints after {self.MAX_RETRIES} attempts")
        valid_constraints = [c for idx, c in enumerate(constraints) if idx not in invalid_indices]
        self.refiner.context.extracted_constraints = valid_constraints
        from app.refine.states.construct_dsl_state import ConstructDSLState
        self.refiner.prompt_state = ConstructDSLState(self.refiner)
