"""
DSL解析器包

包含所有与DSL解析和验证相关的模块：

核心模块：
- dsl_ast.py: DSL AST数据结构定义
- dsl_parser.py: DSL解析器（基于ANTLR，包含DSLParserANTLR类）
- dsl_metadata.py: DSL元数据（节点类型、属性定义等）

验证器子包（validators/）：
- semantic_validator.py: DSL语义验证器
- constraint_validator.py: 约束验证器
- fix_suggester.py: 修复建议生成器

语法定义：
- DSL.g4: ANTLR语法定义文件
"""

# ============================================================================
# AST数据结构
# ============================================================================
from app.refine.parser.dsl_ast import (
    # 主要AST节点
    Query,
    Condition,
    EntityDecl,
    Attribute,
    ValueMatch,
    RelMatch,
    AtomicCondition,
    # 枚举类型
    ConditionType,
    AtomicCondType,
)

# ============================================================================
# DSL解析器
# ============================================================================
# 导入ANTLR解析器（如果可用）
try:
    from app.refine.parser.dsl_parser import DSLParserANTLR
    # 将DSLParserANTLR作为默认的DSLParser（简化使用）
    DSLParser = DSLParserANTLR
except (ImportError, RuntimeError) as e:
    # ANTLR解析器不可用
    import warnings
    warnings.warn(
        f"ANTLR parser not available: {e}. "
        "Please install antlr4-python3-runtime and generate parser from DSL.g4",
        ImportWarning
    )
    DSLParser = None
    DSLParserANTLR = None

# ============================================================================
# DSL元数据
# ============================================================================
from app.refine.parser.dsl_metadata import (
    VALID_NODE_TYPES,
    VALID_PROPERTIES,
    NODE_TYPE_TO_PROPERTIES,
    UNSUPPORTED_PROPERTY_PATHS,
    PROPERTY_VALUE_TYPE_HINTS,
    OPERATOR_TO_NODE_TYPE,
)

# ============================================================================
# 验证器（从validators子包导入）
# ============================================================================
from app.refine.parser.validators import (
    # 验证结果
    ValidationErrorType,
    ValidationError,
    ValidationResult,
    # 语义验证器
    DSLValidator,
    # 约束验证器
    ConstraintValidator,
    # 修复建议生成器
    DSLFixSuggester,
)

# ============================================================================
# 导出列表
# ============================================================================
__all__ = [
    # ===== AST数据结构 =====
    "Query",
    "Condition",
    "EntityDecl",
    "Attribute",
    "ValueMatch",
    "RelMatch",
    "AtomicCondition",
    "ConditionType",
    "AtomicCondType",

    # ===== 解析器 =====
    "DSLParser",          # 简化的别名（推荐使用）
    "DSLParserANTLR",     # 原始类名（向后兼容）

    # ===== 元数据 =====
    "VALID_NODE_TYPES",
    "VALID_PROPERTIES",
    "NODE_TYPE_TO_PROPERTIES",
    "UNSUPPORTED_PROPERTY_PATHS",
    "PROPERTY_VALUE_TYPE_HINTS",
    "OPERATOR_TO_NODE_TYPE",

    # ===== 验证相关 =====
    "ValidationErrorType",
    "ValidationError",
    "ValidationResult",
    "DSLValidator",
    "ConstraintValidator",
    "DSLFixSuggester",
]