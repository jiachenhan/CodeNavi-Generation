"""
DSL验证器子包

包含所有与DSL验证相关的模块：

1. semantic_validator.py: DSL语义验证器
   - 验证Query/Condition的语义正确性
   - 检查节点类型、属性路径、属性值

2. constraint_validator.py: 约束验证器
   - 验证ExtraConstraint的合法性
   - 使用责任链模式组织验证规则
   - 包含语法验证和语义验证

3. fix_suggester.py: 修复建议生成器
   - 为验证错误生成修复建议
   - 自动修复常见错误模式
"""

# 导出语义验证相关
from app.refine.parser.validators.semantic_validator import (
    ValidationErrorType,
    ValidationError,
    ValidationResult,
    DSLValidator,
)

# 导出约束验证相关
from app.refine.parser.validators.constraint_validator import (
    ConstraintValidator,
    ConstraintValidationRule,
    ValidationContext,
    # 语法验证规则
    PathFormatRule,
    AliasExistsRule,
    NoSubQueryOperatorRule,
    # 语义验证规则
    ASTConversionRule,
    DSLSyntaxRule,
    DSLSemanticRule,
)

# 导出修复建议生成器
from app.refine.parser.validators.fix_suggester import (
    DSLFixSuggester,
)

__all__ = [
    # 验证结果相关
    "ValidationErrorType",
    "ValidationError",
    "ValidationResult",

    # 语义验证器
    "DSLValidator",

    # 约束验证器
    "ConstraintValidator",
    "ConstraintValidationRule",
    "ValidationContext",

    # 验证规则（高级用法，一般不需要直接使用）
    "PathFormatRule",
    "AliasExistsRule",
    "NoSubQueryOperatorRule",
    "ASTConversionRule",
    "DSLSyntaxRule",
    "DSLSemanticRule",

    # 修复建议生成器
    "DSLFixSuggester",
]
