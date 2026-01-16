"""
DSL修复建议生成器

根据验证错误生成具体的修复建议，包括：
1. 自动修复不支持的属性路径
2. 将操作符检查转换为节点类型检查
"""
from typing import Optional
from app.refine.parser.dsl_ast import Query, Condition, AtomicCondition, ValueMatch, Attribute
from app.refine.parser.dsl_validator import ValidationError, ValidationErrorType
from app.refine.parser.dsl_metadata import (
    UNSUPPORTED_PROPERTY_PATHS,
    PROPERTY_VALUE_TYPE_HINTS,
)


class DSLFixSuggester:
    """DSL修复建议生成器"""
    
    @staticmethod
    def suggest_fix_for_error(error: ValidationError, query: Query) -> Optional[str]:
        """
        为验证错误生成修复建议
        
        Args:
            error: 验证错误
            query: 查询对象
            
        Returns:
            修复后的DSL片段，如果无法修复则返回None
        """
        if error.error_type == ValidationErrorType.UNSUPPORTED_PROPERTY_PATH:
            return DSLFixSuggester._fix_unsupported_property_path(error, query)
        elif error.error_type == ValidationErrorType.INVALID_VALUE_FOR_PROPERTY:
            return DSLFixSuggester._fix_invalid_property_value(error, query)
        
        return None
    
    @staticmethod
    def _fix_unsupported_property_path(error: ValidationError, query: Query) -> Optional[str]:
        """
        修复不支持的属性路径
        
        例如：binaryoperation_1.operator is instanceof
        -> binaryoperation_1 is instanceofExpression
        """
        if error.suggestion:
            # 从错误消息中提取信息
            # 消息格式：Property 'operator' is not supported on node type 'binaryOperation'
            # 需要找到对应的ValueMatch并修复
            
            # 遍历查询找到对应的条件
            condition = DSLFixSuggester._find_condition_with_property(query.condition, "operator")
            if condition and condition.atomic and condition.atomic.value_match:
                value_match = condition.atomic.value_match
                
                # 检查是否是operator属性
                if value_match.attribute.properties and value_match.attribute.properties[0] == "operator":
                    # 检查值是否是操作符，需要转换为节点类型
                    if value_match.value in PROPERTY_VALUE_TYPE_HINTS.get("operator", {}):
                        suggested_node_type = PROPERTY_VALUE_TYPE_HINTS["operator"][value_match.value]
                        # 生成修复后的条件
                        alias = value_match.attribute.alias
                        return f"{alias} is {suggested_node_type}"
        
        return None
    
    @staticmethod
    def _fix_invalid_property_value(error: ValidationError, query: Query) -> Optional[str]:
        """修复无效的属性值"""
        if error.suggestion:
            # 建议已经包含在error.suggestion中
            return error.suggestion
        
        return None
    
    @staticmethod
    def _find_condition_with_property(condition: Condition, property_name: str) -> Optional[Condition]:
        """查找包含指定属性的条件"""
        if condition.atomic:
            if condition.atomic.value_match:
                attr = condition.atomic.value_match.attribute
                if attr.properties and attr.properties[0] == property_name:
                    return condition
            elif condition.atomic.rel_match:
                attr = condition.atomic.rel_match.attribute
                if attr.properties and attr.properties[0] == property_name:
                    return condition
        
        # 递归查找子条件
        for sub_condition in condition.sub_conditions:
            result = DSLFixSuggester._find_condition_with_property(sub_condition, property_name)
            if result:
                return result
        
        return None
    
    @staticmethod
    def generate_fix_message(errors: list[ValidationError]) -> str:
        """
        生成修复建议消息
        
        Args:
            errors: 验证错误列表
            
        Returns:
            格式化的修复建议消息
        """
        if not errors:
            return ""
        
        messages = []
        messages.append("DSL Validation Errors Found:\n")
        
        for i, error in enumerate(errors, 1):
            messages.append(f"{i}. {error.message}")
            if error.suggestion:
                messages.append(f"   Suggestion: {error.suggestion}")
            messages.append("")
        
        return "\n".join(messages)
