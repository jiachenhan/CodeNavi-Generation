"""
约束验证器 - 使用责任链模式组织验证规则

验证分为两大类：
1. 语法验证（Syntactic Validation）：检查约束的格式和结构
2. 语义验证（Semantic Validation）：检查约束的含义和合法性
"""
from typing import Optional, Dict, List
from abc import ABC, abstractmethod
import re

from app.refine.parser.dsl_ast import Query, Condition
from app.refine.parser.validators.semantic_validator import ValidationError, ValidationErrorType, ValidationResult, DSLValidator
from app.refine.parser import DSLParser


class ConstraintValidationRule(ABC):
    """约束验证规则的抽象基类"""

    def __init__(self):
        self.next_rule: Optional['ConstraintValidationRule'] = None

    def set_next(self, rule: 'ConstraintValidationRule') -> 'ConstraintValidationRule':
        """设置下一个验证规则（责任链模式）"""
        self.next_rule = rule
        return rule

    def validate(self, constraint, context: 'ValidationContext') -> Optional[ValidationResult]:
        """
        验证约束，如果验证失败返回ValidationResult，否则传递给下一个规则

        Args:
            constraint: ExtraConstraint对象
            context: 验证上下文

        Returns:
            ValidationResult如果验证失败，None如果通过验证
        """
        result = self._do_validate(constraint, context)
        if result is not None and not result.is_valid:
            return result

        # 如果当前规则通过，继续下一个规则
        if self.next_rule:
            return self.next_rule.validate(constraint, context)

        # 所有规则都通过
        return ValidationResult(is_valid=True, errors=[], warnings=[])

    @abstractmethod
    def _do_validate(self, constraint, context: 'ValidationContext') -> Optional[ValidationResult]:
        """子类实现具体的验证逻辑"""
        pass


class ValidationContext:
    """验证上下文 - 存储验证过程中需要的信息"""

    def __init__(self, original_dsl: str):
        self.original_dsl = original_dsl
        self.node_map: Dict[str, Query] = {}
        self.original_parser: Optional[DSLParser] = None
        self.original_query: Optional[Query] = None

    def ensure_parsed(self) -> bool:
        """确保原始DSL已被解析"""
        if self.original_parser is None:
            self.original_parser = DSLParser(self.original_dsl)
            self.original_query = self.original_parser.parse()
            if not self.original_query:
                return False
            self.node_map = self.original_parser.node_map if hasattr(self.original_parser, 'node_map') else {}
        return True


# ============================================================================
# 语法验证规则（Syntactic Validation Rules）
# ============================================================================

class PathFormatRule(ConstraintValidationRule):
    """验证constraint_path的格式"""

    def _do_validate(self, constraint, context: ValidationContext) -> Optional[ValidationResult]:
        if not constraint.constraint_path:
            return ValidationResult(
                is_valid=False,
                errors=[ValidationError(
                    error_type=ValidationErrorType.MISSING_ALIAS,
                    message="constraint_path is empty"
                )],
                warnings=[]
            )

        # constraint_path格式应该是 "alias" 或 "alias.property" 或 "alias.property.subproperty"
        parts = constraint.constraint_path.split('.')
        if not parts or not parts[0]:
            return ValidationResult(
                is_valid=False,
                errors=[ValidationError(
                    error_type=ValidationErrorType.MISSING_ALIAS,
                    message="Invalid constraint_path format: must be 'alias' or 'alias.property'"
                )],
                warnings=[]
            )

        return None  # 通过验证


class AliasExistsRule(ConstraintValidationRule):
    """验证别名是否在原始DSL中存在"""

    def _do_validate(self, constraint, context: ValidationContext) -> Optional[ValidationResult]:
        if not context.ensure_parsed():
            return ValidationResult(
                is_valid=False,
                errors=[ValidationError(
                    error_type=ValidationErrorType.INVALID_NODE_TYPE,
                    message="Failed to parse original DSL"
                )],
                warnings=[]
            )

        alias = constraint.constraint_path.split('.')[0]
        if alias not in context.node_map:
            available_aliases = list(context.node_map.keys())
            return ValidationResult(
                is_valid=False,
                errors=[ValidationError(
                    error_type=ValidationErrorType.MISSING_ALIAS,
                    message=f"Alias '{alias}' is not defined in original DSL",
                    suggestion=f"Available aliases: {', '.join(available_aliases)}" if available_aliases else "No aliases found"
                )],
                warnings=[]
            )

        return None  # 通过验证


class NoSubQueryOperatorRule(ConstraintValidationRule):
    """
    验证不使用创建子查询的操作符（contain/in/notIn）

    这类错误是不可修复的，应该直接丢弃约束
    """

    def _do_validate(self, constraint, context: ValidationContext) -> Optional[ValidationResult]:
        if constraint.operator not in ('contain', 'in', 'notIn'):
            return None  # 不是子查询操作符，通过验证

        # 使用子查询操作符是不可修复的错误 - 应该直接丢弃约束
        available = ', '.join(context.node_map.keys()) if context.node_map else "none"

        return ValidationResult(
            is_valid=False,
            errors=[ValidationError(
                error_type=ValidationErrorType.INVALID_VALUE_FOR_PROPERTY,
                message=f"Operator '{constraint.operator}' creates sub-queries and is not allowed in constraints",
                suggestion=f"To add constraints to existing nodes, use:\n- Path: <alias>.property (e.g., node_1.name)\n- Operator: ==, !=, match, is\n- Available aliases: {available}",
                should_discard_constraint=True  # 标记为应该丢弃
            )],
            warnings=[]
        )


# ============================================================================
# 语义验证规则（Semantic Validation Rules）
# ============================================================================

class ASTConversionRule(ConstraintValidationRule):
    """验证约束能否正确转换为AST"""

    def _do_validate(self, constraint, context: ValidationContext) -> Optional[ValidationResult]:
        # 需要导入这个函数
        from app.refine.dsl_constructor import constraint_to_condition

        condition = constraint_to_condition(constraint)
        if not condition:
            return ValidationResult(
                is_valid=False,
                errors=[ValidationError(
                    error_type=ValidationErrorType.INVALID_VALUE_FOR_PROPERTY,
                    message="Failed to convert constraint to Condition AST",
                    suggestion="Check that operator and value are compatible"
                )],
                warnings=[]
            )

        # 将condition存储到context中供后续规则使用
        context.converted_condition = condition
        return None  # 通过验证


class DSLSyntaxRule(ConstraintValidationRule):
    """验证生成的DSL条件语法正确"""

    def _do_validate(self, constraint, context: ValidationContext) -> Optional[ValidationResult]:
        from app.refine.dsl_constructor import condition_to_dsl

        # 使用之前转换的condition
        if not hasattr(context, 'converted_condition'):
            return ValidationResult(
                is_valid=False,
                errors=[ValidationError(
                    error_type=ValidationErrorType.INVALID_VALUE_FOR_PROPERTY,
                    message="Internal error: condition not found in context"
                )],
                warnings=[]
            )

        condition_dsl = condition_to_dsl(context.converted_condition)
        if not condition_dsl:
            return ValidationResult(
                is_valid=False,
                errors=[ValidationError(
                    error_type=ValidationErrorType.INVALID_VALUE_FOR_PROPERTY,
                    message="Failed to convert condition to DSL string"
                )],
                warnings=[]
            )

        # 解析DSL条件
        context.ensure_parsed()
        condition_parser = DSLParser(condition_dsl)
        parsed_condition = condition_parser.parse_condition(context.node_map)

        if not parsed_condition:
            parse_errors = condition_parser.get_parse_errors()
            error_messages = []
            if hasattr(parse_errors, 'errors'):
                error_messages = [str(e) for e in parse_errors.errors]
            else:
                error_messages = ["Parse failed with unknown error"]

            return ValidationResult(
                is_valid=False,
                errors=[ValidationError(
                    error_type=ValidationErrorType.INVALID_VALUE_FOR_PROPERTY,
                    message=f"Syntax error in generated DSL: {'; '.join(error_messages)}"
                )],
                warnings=[]
            )

        # 将解析后的condition存储供下一步使用
        context.parsed_condition = parsed_condition
        return None  # 通过验证


class DSLSemanticRule(ConstraintValidationRule):
    """验证DSL的语义正确性（属性、类型等）"""

    def _do_validate(self, constraint, context: ValidationContext) -> Optional[ValidationResult]:
        # 使用之前解析的condition和node_map
        if not hasattr(context, 'parsed_condition'):
            return ValidationResult(
                is_valid=False,
                errors=[ValidationError(
                    error_type=ValidationErrorType.INVALID_VALUE_FOR_PROPERTY,
                    message="Internal error: parsed condition not found in context"
                )],
                warnings=[]
            )

        context.ensure_parsed()
        validator = DSLValidator()
        validation_result = validator.validate_condition(context.parsed_condition, context.node_map)

        return validation_result if not validation_result.is_valid else None


# ============================================================================
# 约束验证器 - 组装所有验证规则
# ============================================================================

class ConstraintValidator:
    """约束验证器 - 使用责任链模式组织所有验证规则"""

    def __init__(self):
        # 构建验证链：语法验证 -> 语义验证
        self.validation_chain = self._build_validation_chain()

    def _build_validation_chain(self) -> ConstraintValidationRule:
        """构建验证规则链"""
        # 语法验证规则
        path_format = PathFormatRule()
        alias_exists = AliasExistsRule()
        no_subquery = NoSubQueryOperatorRule()

        # 语义验证规则
        ast_conversion = ASTConversionRule()
        dsl_syntax = DSLSyntaxRule()
        dsl_semantic = DSLSemanticRule()

        # 链接规则：按顺序执行
        # 1. 先检查路径格式
        # 2. 再检查别名是否存在
        # 3. 检查是否使用了子查询操作符
        # 4. 转换为AST
        # 5. 验证DSL语法
        # 6. 验证DSL语义
        path_format.set_next(alias_exists) \
                   .set_next(no_subquery) \
                   .set_next(ast_conversion) \
                   .set_next(dsl_syntax) \
                   .set_next(dsl_semantic)

        return path_format

    def validate(self, constraint, original_dsl: str) -> ValidationResult:
        """
        验证约束的合法性

        Args:
            constraint: ExtraConstraint对象
            original_dsl: 原始DSL代码

        Returns:
            ValidationResult对象
        """
        try:
            context = ValidationContext(original_dsl)
            result = self.validation_chain.validate(constraint, context)
            return result if result else ValidationResult(is_valid=True, errors=[], warnings=[])
        except Exception as e:
            return ValidationResult(
                is_valid=False,
                errors=[ValidationError(
                    error_type=ValidationErrorType.INVALID_VALUE_FOR_PROPERTY,
                    message=f"Validation error: {str(e)}"
                )],
                warnings=[]
            )
