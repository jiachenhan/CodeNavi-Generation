"""
DSL语义验证器

验证DSL的语义正确性，包括：
1. 节点类型是否有效
2. 属性路径是否有效
3. 属性值是否合理
4. 提供修复建议
"""
from typing import List, Optional, Dict, Tuple
from dataclasses import dataclass
from enum import Enum

from app.refine.parser.dsl_ast import Query, Condition, AtomicCondition, ValueMatch, RelMatch, Attribute
from app.refine.parser.dsl_metadata import (
    VALID_NODE_TYPES,
    VALID_PROPERTIES,
    NODE_TYPE_TO_PROPERTIES,
    UNSUPPORTED_PROPERTY_PATHS,
    PROPERTY_VALUE_TYPE_HINTS,
)


class ValidationErrorType(Enum):
    """验证错误类型"""
    INVALID_NODE_TYPE = "invalid_node_type"
    INVALID_PROPERTY = "invalid_property"
    UNSUPPORTED_PROPERTY_PATH = "unsupported_property_path"
    INVALID_VALUE_FOR_PROPERTY = "invalid_value_for_property"
    MISSING_ALIAS = "missing_alias"
    DUPLICATE_ALIAS = "duplicate_alias"  # 在contain/in子句中重复使用已存在的别名


@dataclass
class ValidationError:
    """验证错误"""
    error_type: ValidationErrorType
    message: str
    suggestion: Optional[str] = None
    location: Optional[Tuple[int, int]] = None  # (start_pos, end_pos)


@dataclass
class ValidationResult:
    """验证结果"""
    is_valid: bool
    errors: List[ValidationError]
    warnings: List[str] = None

    def __post_init__(self):
        if self.warnings is None:
            self.warnings = []


class DSLValidator:
    """DSL语义验证器"""
    
    def __init__(self):
        self.node_map: Dict[str, Query] = {}  # alias -> Query
        self.errors: List[ValidationError] = []
        self.warnings: List[str] = []
    
    def validate(self, query: Query) -> ValidationResult:
        """
        验证DSL查询

        Args:
            query: 要验证的查询

        Returns:
            验证结果
        """
        self.errors = []
        self.warnings = []
        self.node_map = {}

        # 收集所有节点（包括嵌套查询）
        self._collect_nodes(query)

        # 验证查询
        self._validate_query(query)

        return ValidationResult(
            is_valid=len(self.errors) == 0,
            errors=self.errors,
            warnings=self.warnings
        )

    def validate_condition(self, condition: Condition, node_map: Dict[str, Query]) -> ValidationResult:
        """
        验证独立的Condition（不需要完整Query）

        Args:
            condition: 要验证的条件
            node_map: 节点别名到Query的映射（来自原始DSL）

        Returns:
            验证结果
        """
        self.errors = []
        self.warnings = []
        self.node_map = node_map  # 使用提供的node_map

        # 从node_map中提取上下文信息
        # 假设验证的条件属于第一个节点（通常是根节点）
        if node_map:
            first_query = next(iter(node_map.values()))
            context_node_type = first_query.entity.node_type
            context_alias = first_query.entity.alias
        else:
            # 如果没有提供node_map，使用默认值
            context_node_type = "unknown"
            context_alias = None

        # 验证条件
        self._validate_condition(condition, context_node_type, context_alias)

        return ValidationResult(
            is_valid=len(self.errors) == 0,
            errors=self.errors,
            warnings=self.warnings
        )
    
    def _collect_nodes(self, query: Query):
        """收集所有节点到node_map"""
        if query.entity.alias:
            self.node_map[query.entity.alias] = query
        
        # 递归收集嵌套查询中的节点
        self._collect_nodes_from_condition(query.condition)
    
    def _collect_nodes_from_condition(self, condition: Condition):
        """从条件中收集节点"""
        if condition.atomic:
            if condition.atomic.rel_match:
                # 嵌套查询
                nested_query = condition.atomic.rel_match.query
                if nested_query.entity.alias:
                    self.node_map[nested_query.entity.alias] = nested_query
                if nested_query.condition:
                    self._collect_nodes_from_condition(nested_query.condition)
        
        # 递归处理子条件
        for sub_condition in condition.sub_conditions:
            self._collect_nodes_from_condition(sub_condition)
    
    def _validate_query(self, query: Query):
        """验证查询"""
        # 验证节点类型
        node_type = query.entity.node_type
        if node_type not in VALID_NODE_TYPES:
            self.errors.append(ValidationError(
                error_type=ValidationErrorType.INVALID_NODE_TYPE,
                message=f"Invalid node type: {node_type}",
                suggestion=f"Valid node types include: {', '.join(sorted(VALID_NODE_TYPES)[:10])}..."
            ))
        
        # 验证条件
        self._validate_condition(query.condition, query.entity.node_type, query.entity.alias)
    
    def _validate_condition(self, condition: Condition, context_node_type: str, context_alias: Optional[str]):
        """验证条件"""
        if condition.atomic:
            self._validate_atomic_condition(condition.atomic, context_node_type, context_alias)
        
        # 递归验证子条件
        for sub_condition in condition.sub_conditions:
            self._validate_condition(sub_condition, context_node_type, context_alias)
    
    def _validate_atomic_condition(self, atomic: AtomicCondition, context_node_type: str, context_alias: Optional[str]):
        """验证原子条件"""
        if atomic.value_match:
            self._validate_value_match(atomic.value_match, context_node_type, context_alias)
        elif atomic.rel_match:
            self._validate_rel_match(atomic.rel_match, context_node_type, context_alias)
    
    def _validate_value_match(self, value_match: ValueMatch, context_node_type: str, context_alias: Optional[str]):
        """验证值匹配"""
        attribute = value_match.attribute
        
        # 验证属性路径
        self._validate_attribute(attribute, context_node_type, context_alias)
        
        # 如果属性路径有效，验证值
        if attribute.alias in self.node_map:
            target_node = self.node_map[attribute.alias]
            target_node_type = target_node.entity.node_type
            
            # 检查属性路径
            if attribute.properties:
                first_prop = attribute.properties[0]
                
                # 检查是否是不支持的属性路径
                if (target_node_type, first_prop) in UNSUPPORTED_PROPERTY_PATHS:
                    # 特殊处理：如果是operator属性，检查值是否是操作符，需要转换为节点类型
                    if first_prop == "operator" and value_match.operator == "is":
                        # 检查值是否在操作符到节点类型的映射中
                        from app.refine.parser.dsl_metadata import OPERATOR_TO_NODE_TYPE
                        if value_match.value in OPERATOR_TO_NODE_TYPE:
                            suggested_node_type = OPERATOR_TO_NODE_TYPE[value_match.value]
                            suggestion = f"{attribute.alias} is {suggested_node_type}"
                        else:
                            suggestion = f"Property 'operator' is not supported on '{target_node_type}'. Use node type check instead: {attribute.alias} is <nodeType>"
                    else:
                        suggestion = self._generate_fix_suggestion(
                            target_node_type, first_prop, value_match.value, value_match.operator, attribute.alias
                        )
                    self.errors.append(ValidationError(
                        error_type=ValidationErrorType.UNSUPPORTED_PROPERTY_PATH,
                        message=f"Property '{first_prop}' is not supported on node type '{target_node_type}'. "
                               f"Found: {attribute.alias}.{first_prop} {value_match.operator} {value_match.value}",
                        suggestion=suggestion
                    ))
                    return
                
                # 检查属性是否有效
                valid_props = NODE_TYPE_TO_PROPERTIES.get(target_node_type, set())
                if first_prop not in valid_props and first_prop not in VALID_PROPERTIES:
                    # 可能是无效属性，但先不报错，因为映射可能不完整
                    self.warnings.append(
                        f"Property '{first_prop}' on node type '{target_node_type}' may not be valid. "
                        f"Valid properties for this node type: {', '.join(sorted(valid_props)) if valid_props else 'none'}"
                    )
                
                # 检查值是否合理
                if value_match.operator == "is" and value_match.value in VALID_NODE_TYPES:
                    # 使用is操作符检查节点类型，这是合理的
                    pass
                elif first_prop in PROPERTY_VALUE_TYPE_HINTS:
                    # 检查是否需要修复值
                    hints = PROPERTY_VALUE_TYPE_HINTS[first_prop]
                    if value_match.value in hints:
                        suggested_value = hints[value_match.value]
                        suggestion = self._generate_fix_suggestion(
                            target_node_type, first_prop, suggested_value, value_match.operator, attribute.alias
                        )
                        self.errors.append(ValidationError(
                            error_type=ValidationErrorType.INVALID_VALUE_FOR_PROPERTY,
                            message=f"Value '{value_match.value}' for property '{first_prop}' should be replaced with node type check",
                            suggestion=suggestion
                        ))
    
    def _validate_rel_match(self, rel_match: RelMatch, context_node_type: str, context_alias: Optional[str]):
        """验证关系匹配"""
        attribute = rel_match.attribute
        
        # 验证属性路径
        self._validate_attribute(attribute, context_node_type, context_alias)
        
        # 验证嵌套查询
        if rel_match.query:
            self._validate_query(rel_match.query)
    
    def _validate_attribute(self, attribute: Attribute, context_node_type: str, context_alias: Optional[str]):
        """验证属性"""
        # 检查别名是否存在
        if attribute.alias not in self.node_map:
            # 可能是上下文别名
            if attribute.alias == context_alias:
                # 使用上下文节点类型
                target_node_type = context_node_type
            else:
                self.errors.append(ValidationError(
                    error_type=ValidationErrorType.MISSING_ALIAS,
                    message=f"Alias '{attribute.alias}' is not defined",
                    suggestion=f"Make sure to declare the node with alias '{attribute.alias}' before using it"
                ))
                return
        else:
            target_node = self.node_map[attribute.alias]
            target_node_type = target_node.entity.node_type
        
        # 验证属性路径
        if attribute.properties:
            first_prop = attribute.properties[0]
            
            # 检查是否是不支持的属性路径
            if (target_node_type, first_prop) in UNSUPPORTED_PROPERTY_PATHS:
                self.errors.append(ValidationError(
                    error_type=ValidationErrorType.UNSUPPORTED_PROPERTY_PATH,
                    message=f"Property '{first_prop}' is not supported on node type '{target_node_type}'",
                    suggestion=f"Consider using node type check instead: {attribute.alias} is <nodeType>"
                ))
    
    def _generate_fix_suggestion(self, node_type: str, property_name: str, value: str, operator: str, alias: str = None) -> str:
        """
        生成修复建议
        
        例如：
        - binaryoperation_1.operator is instanceof -> binaryoperation_1 is instanceofExpression
        """
        # 如果是不支持的属性路径，建议使用节点类型检查
        if (node_type, property_name) in UNSUPPORTED_PROPERTY_PATHS:
            # 检查值是否是操作符，需要转换为节点类型
            if property_name == "operator" and value in PROPERTY_VALUE_TYPE_HINTS.get("operator", {}):
                suggested_node_type = PROPERTY_VALUE_TYPE_HINTS["operator"][value]
                if alias:
                    return f"{alias} is {suggested_node_type}"
                else:
                    return f"<alias> is {suggested_node_type}"
            else:
                if alias:
                    return f"Property '{property_name}' is not supported on {node_type}. Consider using: {alias} is <nodeType>"
                else:
                    return f"Property '{property_name}' is not supported. Consider using node type check: <alias> is <nodeType>"
        
        return ""
