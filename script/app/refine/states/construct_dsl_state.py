"""
ConstructDSLState - DSL构造状态

Step4: 根据验证通过的约束构造最终的refined DSL
"""
from app.refine.states.base_state import PromptState
from app.refine.dsl_constructor import merge_constraints_to_dsl
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


# ============================================================================
# 辅助类 - DSL构造器Helper
# ============================================================================

class DSLConstructorHelper:
    """DSL构造辅助类 - 封装DSL构造逻辑"""

    @staticmethod
    def construct(original_dsl: str, constraints: list) -> str:
        """
        根据约束构造refined DSL

        Args:
            original_dsl: 原始DSL代码
            constraints: 约束列表

        Returns:
            构造后的DSL代码
        """
        try:
            refined_dsl = merge_constraints_to_dsl(
                original_dsl=original_dsl,
                constraints=constraints
            )
            return refined_dsl
        except Exception as e:
            _logger.error(f"Error constructing DSL: {e}")
            # 如果构造失败，返回原始DSL
            _logger.warning("Returning original DSL due to construction failure")
            return original_dsl


# ============================================================================
# 主State类
# ============================================================================

class ConstructDSLState(PromptState):
    """Step4: 构造refined DSL状态"""

    def accept(self):
        """执行DSL构造"""
        _logger.info("Constructing refined DSL based on extracted constraints...")

        original_dsl = self.refiner.input_data.dsl_code
        constraints = self.refiner.context.extracted_constraints

        if not constraints:
            _logger.warning("No valid constraints to apply, using original DSL")
            self.refiner.refined_dsl = original_dsl
        else:
            _logger.info(f"Applying {len(constraints)} constraints to original DSL")
            refined_dsl = DSLConstructorHelper.construct(original_dsl, constraints)
            self.refiner.refined_dsl = refined_dsl

        _logger.info("DSL construction completed")

        # 转换到退出状态
        from app.refine.states.common_states import ExitState
        self.refiner.prompt_state = ExitState(self.refiner)
