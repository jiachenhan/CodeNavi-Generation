"""
DSL解析器子包

包含所有与DSL语法解析相关的模块：
- dsl_ast.py: DSL AST数据结构定义
- dsl_parser_antlr.py: 基于ANTLR的解析器
- DSL.g4: ANTLR语法定义文件
"""
from app.refine.parser.dsl_ast import (
    Query,
    Condition,
    EntityDecl,
    Attribute,
    ValueMatch,
    RelMatch,
    AtomicCondition,
    ConditionType,
    AtomicCondType,
)

# 导入ANTLR解析器（如果可用）
try:
    from app.refine.parser.dsl_parser_antlr import DSLParserANTLR
    # 将DSLParserANTLR作为默认的DSLParser
    DSLParser = DSLParserANTLR
    __all__ = [
        "DSLParser",
        "DSLParserANTLR",
        "Query",
        "Condition",
        "EntityDecl",
        "Attribute",
        "ValueMatch",
        "RelMatch",
        "AtomicCondition",
        "ConditionType",
        "AtomicCondType",
    ]
except (ImportError, RuntimeError) as e:
    # ANTLR解析器不可用
    import warnings
    warnings.warn(
        f"ANTLR parser not available: {e}. "
        "Please install antlr4-python3-runtime and generate parser from DSL.g4",
        ImportWarning
    )
    DSLParser = None
    __all__ = [
        "Query",
        "Condition",
        "EntityDecl",
        "Attribute",
        "ValueMatch",
        "RelMatch",
        "AtomicCondition",
        "ConditionType",
        "AtomicCondType",
    ]