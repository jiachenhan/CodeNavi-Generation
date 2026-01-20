"""
DSL构造辅助工具：将额外约束转换为DSL条件子句

使用DSL解析器进行解析和修改，而不是基于字符串的正则表达式操作。
这样可以确保语法正确性，并更好地处理嵌套结构。
"""
from typing import List, Dict, Optional, Tuple
from app.refine.data_structures import ExtraConstraint, ConstraintType
from app.refine.parser import (
    DSLParser, Query, Condition, EntityDecl, Attribute,
    ValueMatch, RelMatch, AtomicCondition,
    ConditionType, AtomicCondType
)
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def query_to_dsl(query: Query, is_nested: bool = False) -> str:
    """
    将Query AST转换为DSL字符串
    
    Args:
        query: Query AST对象
        is_nested: 是否为嵌套查询（嵌套查询不需要分号）
    
    Returns:
        DSL字符串
    """
    # 实体声明
    entity_str = query.entity.node_type
    if query.entity.alias:
        entity_str += f" {query.entity.alias}"
    
    # 条件字符串
    condition_str = condition_to_dsl(query.condition)
    
    # 组合
    if condition_str:
        dsl = f"{entity_str} where {condition_str}"
    else:
        dsl = entity_str
    
    # 嵌套查询不需要分号，主查询需要分号
    if not is_nested:
        dsl += " ;"
    
    return dsl


def condition_to_dsl(condition: Condition) -> str:
    """
    将Condition AST转换为DSL字符串
    
    Args:
        condition: Condition AST对象
    
    Returns:
        DSL条件字符串
    """
    if condition.type == ConditionType.ATOMIC:
        if condition.atomic:
            return atomic_condition_to_dsl(condition.atomic)
        return ""
    
    elif condition.type == ConditionType.NOT:
        if condition.sub_conditions:
            sub = condition_to_dsl(condition.sub_conditions[0])
            return f"not({sub})"
        return ""
    
    elif condition.type == ConditionType.AND:
        if condition.sub_conditions:
            subs = [condition_to_dsl(c) for c in condition.sub_conditions if c]
            subs = [s for s in subs if s]
            if subs:
                return f"and({', '.join(subs)})"
        return ""
    
    elif condition.type == ConditionType.OR:
        if condition.sub_conditions:
            subs = [condition_to_dsl(c) for c in condition.sub_conditions if c]
            subs = [s for s in subs if s]
            if subs:
                return f"or({', '.join(subs)})"
        return ""
    
    return ""


def atomic_condition_to_dsl(atomic: AtomicCondition) -> str:
    """
    将AtomicCondition AST转换为DSL字符串
    
    Args:
        atomic: AtomicCondition AST对象
    
    Returns:
        DSL原子条件字符串
    """
    if atomic.type == AtomicCondType.VALUE_MATCH and atomic.value_match:
        vm = atomic.value_match
        attr_str = attribute_to_dsl(vm.attribute)
        value_str = escape_value(vm.value)
        return f"{attr_str} {vm.operator} {value_str}"
    
    elif atomic.type == AtomicCondType.REL_MATCH and atomic.rel_match:
        rm = atomic.rel_match
        attr_str = attribute_to_dsl(rm.attribute)
        query_str = query_to_dsl(rm.query, is_nested=True)
        return f"{attr_str} {rm.operator} {query_str}"
    
    return ""


def attribute_to_dsl(attr: Attribute) -> str:
    """
    将Attribute AST转换为DSL字符串
    
    Args:
        attr: Attribute AST对象
    
    Returns:
        DSL属性字符串
    """
    if attr.properties:
        return f"{attr.alias}.{'.'.join(attr.properties)}"
    return attr.alias


def escape_value(value: str) -> str:
    """
    转义值字符串（如果是字符串字面量，添加引号；如果是数字或布尔值，直接返回）
    
    Args:
        value: 值字符串
    
    Returns:
        转义后的值字符串
    """
    # 如果已经是带引号的字符串，直接返回
    if (value.startswith('"') and value.endswith('"')) or \
       (value.startswith("'") and value.endswith("'")):
        return value
    
    # 如果是布尔值或数字，直接返回
    if value.lower() in ('true', 'false') or value.replace('.', '').replace('-', '').isdigit():
        return value
    
    # 如果是标识符（节点类型或变量名），直接返回
    if value.replace('_', '').isalnum() and not value[0].isdigit():
        return value
    
    # 否则作为字符串字面量处理，添加双引号并转义内部引号
    # 将 " 转义为 \"，将 ' 转义为 \'
    escaped = value.replace('\\', '\\\\').replace('"', '\\"').replace("'", "\\'")
    return f'"{escaped}"'


def constraint_to_dsl_condition(constraint: ExtraConstraint) -> str:
    """
    将单个约束转换为DSL条件字符串（简化格式）
    
    Args:
        constraint: 额外约束对象（新格式：constraint_path, operator, value）
    
    Returns:
        DSL条件字符串
    """
    if not constraint.constraint_path or not constraint.operator or not constraint.value:
        _logger.warning(f"Invalid constraint: missing required fields")
        return ""
    
    # constraint_path格式：nodeAlias 或 nodeAlias.attribute
    # 直接使用路径和操作符构建条件
    return f"{constraint.constraint_path} {constraint.operator} {constraint.value}"


def constraints_to_dsl_conditions(constraints: List[ExtraConstraint], dsl_node: str) -> str:
    """
    将属于同一DSL节点的多个约束组合为DSL条件
    
    Args:
        constraints: 约束列表
        dsl_node: DSL节点（alias，从constraint_path中提取）
    
    Returns:
        组合后的DSL条件字符串
    """
    # 过滤出属于该节点的约束
    # constraint_path可以是 "nodeAlias" 或 "nodeAlias.attribute"
    node_constraints = [
        c for c in constraints
        if c.constraint_path and (
            c.constraint_path == dsl_node or  # 节点本身的约束（如类型检查）
            c.constraint_path.startswith(f"{dsl_node}.")  # 节点属性的约束
        )
    ]
    
    if not node_constraints:
        return ""
    
    # 将约束转换为条件
    conditions = []
    for constraint in node_constraints:
        condition = constraint_to_dsl_condition(constraint)
        if condition:
            # 如果是负约束，需要取反
            if constraint.is_negative:
                condition = f"not({condition})"
            conditions.append(condition)
    
    if not conditions:
        return ""
    
    # 组合条件
    if len(conditions) == 1:
        return conditions[0]
    else:
        return f"and({', '.join(conditions)})"


def find_condition_by_path(condition: Condition, constraint_path: str, operator: str) -> Optional[AtomicCondition]:
    """
    在条件树中查找匹配指定路径和操作符的原子条件

    Args:
        condition: 条件树
        constraint_path: 约束路径（如 "nodeAlias" 或 "nodeAlias.attribute"）
        operator: 操作符（如 "==", "match", "is"）

    Returns:
        匹配的AtomicCondition，如果未找到则返回None
    """
    if condition.type == ConditionType.ATOMIC:
        if condition.atomic:
            if condition.atomic.type == AtomicCondType.VALUE_MATCH and condition.atomic.value_match:
                vm = condition.atomic.value_match
                attr_path = attribute_to_dsl(vm.attribute)
                if attr_path == constraint_path and vm.operator == operator:
                    return condition.atomic
            elif condition.atomic.type == AtomicCondType.REL_MATCH and condition.atomic.rel_match:
                rm = condition.atomic.rel_match
                attr_path = attribute_to_dsl(rm.attribute)
                if attr_path == constraint_path and rm.operator == operator:
                    return condition.atomic
        return None
    
    # 递归查找子条件
    for sub_condition in condition.sub_conditions:
        result = find_condition_by_path(sub_condition, constraint_path, operator)
        if result:
            return result
    
    return None


def remove_condition_by_path(condition: Condition, constraint_path: str, operator: str, original_value: Optional[str] = None) -> Optional[Condition]:
    """
    从条件树中移除匹配指定路径、操作符和值的原子条件
    
    Args:
        condition: 条件树
        constraint_path: 约束路径
        operator: 操作符
        original_value: 原始值（可选，用于精确匹配）
    
    Returns:
        修改后的条件树，如果条件被移除后为空则返回None
    """
    if condition.type == ConditionType.ATOMIC:
        if condition.atomic:
            if condition.atomic.type == AtomicCondType.VALUE_MATCH and condition.atomic.value_match:
                vm = condition.atomic.value_match
                attr_path = attribute_to_dsl(vm.attribute)
                if attr_path == constraint_path and vm.operator == operator:
                    if original_value is None or vm.value == original_value or escape_value(vm.value) == original_value:
                        return None  # 移除这个条件
        return condition  # 不匹配，保留
    
    elif condition.type == ConditionType.NOT:
        if condition.sub_conditions:
            new_sub = remove_condition_by_path(condition.sub_conditions[0], constraint_path, operator, original_value)
            if new_sub is None:
                return None  # not(condition) 中的条件被移除，整个not也移除
            return Condition(type=ConditionType.NOT, sub_conditions=[new_sub])
        return condition
    
    elif condition.type in (ConditionType.AND, ConditionType.OR):
        new_subs = []
        for sub in condition.sub_conditions:
            new_sub = remove_condition_by_path(sub, constraint_path, operator, original_value)
            if new_sub is not None:
                new_subs.append(new_sub)
        
        if not new_subs:
            return None  # 所有子条件都被移除
        
        if len(new_subs) == 1:
            # 如果只剩一个子条件，去掉and/or包装
            return new_subs[0]
        
        return Condition(type=condition.type, sub_conditions=new_subs)
    
    return condition


def update_condition_by_path(condition: Condition, constraint_path: str, operator: str, new_value: str, original_value: Optional[str] = None) -> Condition:
    """
    在条件树中更新匹配指定路径和操作符的原子条件的值
    
    Args:
        condition: 条件树
        constraint_path: 约束路径
        operator: 操作符
        new_value: 新值
        original_value: 原始值（可选，用于精确匹配）
    
    Returns:
        修改后的条件树
    """
    if condition.type == ConditionType.ATOMIC:
        if condition.atomic:
            if condition.atomic.type == AtomicCondType.VALUE_MATCH and condition.atomic.value_match:
                vm = condition.atomic.value_match
                attr_path = attribute_to_dsl(vm.attribute)
                if attr_path == constraint_path and vm.operator == operator:
                    if original_value is None or vm.value == original_value or escape_value(vm.value) == original_value:
                        # 创建新的ValueMatch
                        new_vm = ValueMatch(
                            attribute=vm.attribute,
                            operator=vm.operator,
                            value=new_value
                        )
                        new_atomic = AtomicCondition(
                            type=AtomicCondType.VALUE_MATCH,
                            value_match=new_vm
                        )
                        return Condition(type=ConditionType.ATOMIC, atomic=new_atomic)
        return condition
    
    # 递归更新子条件
    new_subs = []
    for sub in condition.sub_conditions:
        new_sub = update_condition_by_path(sub, constraint_path, operator, new_value, original_value)
        new_subs.append(new_sub)
    
    return Condition(type=condition.type, sub_conditions=new_subs)


def add_condition_to_tree(condition: Condition, new_condition: Condition, use_and: bool = True) -> Condition:
    """
    将新条件添加到条件树中
    
    Args:
        condition: 现有条件树
        new_condition: 要添加的新条件
        use_and: 是否使用and组合（True）还是or组合（False）
    
    Returns:
        组合后的条件树
    """
    # 如果现有条件为空，直接返回新条件
    if not condition or (condition.type == ConditionType.ATOMIC and not condition.atomic):
        return new_condition
    
    # 如果现有条件已经是相同类型的组合，直接添加
    if condition.type == (ConditionType.AND if use_and else ConditionType.OR):
        new_subs = list(condition.sub_conditions)
        new_subs.append(new_condition)
        return Condition(type=condition.type, sub_conditions=new_subs)
    
    # 否则创建新的组合
    combine_type = ConditionType.AND if use_and else ConditionType.OR
    return Condition(type=combine_type, sub_conditions=[condition, new_condition])


def constraint_to_condition(constraint: ExtraConstraint) -> Optional[Condition]:
    """
    将约束转换为Condition AST
    
    Args:
        constraint: 额外约束对象
    
    Returns:
        Condition AST对象，如果转换失败则返回None
    """
    if not constraint.constraint_path or not constraint.operator:
        return None
    
    # 解析路径
    path_parts = constraint.constraint_path.split('.')
    if not path_parts:
        return None
    
    alias = path_parts[0]
    properties = path_parts[1:] if len(path_parts) > 1 else []
    
    attr = Attribute(alias=alias, properties=properties)
    
    # 判断是值匹配还是关系匹配
    if constraint.operator in ('contain', 'in', 'notIn'):
        # 关系匹配：需要解析value作为子查询
        if not constraint.value:
            return None
        
        # 尝试解析value作为DSL子查询
        try:
            # 对于contain/in，value应该是一个完整的DSL查询（带分号）
            parser = DSLParser(constraint.value)
            sub_query = parser.parse()
            if not sub_query:
                _logger.warning(f"Failed to parse subquery: {constraint.value}")
                return None
            
            rel_match = RelMatch(attribute=attr, operator=constraint.operator, query=sub_query)
            atomic = AtomicCondition(type=AtomicCondType.REL_MATCH, rel_match=rel_match)
            return Condition(type=ConditionType.ATOMIC, atomic=atomic)
        except Exception as e:
            _logger.warning(f"Error parsing subquery '{constraint.value}': {e}")
            return None
    else:
        # 值匹配
        if not constraint.value:
            return None
        
        value_match = ValueMatch(attribute=attr, operator=constraint.operator, value=constraint.value)
        atomic = AtomicCondition(type=AtomicCondType.VALUE_MATCH, value_match=value_match)
        base_condition = Condition(type=ConditionType.ATOMIC, atomic=atomic)
        
        # 如果是负约束，包装在not中
        if constraint.is_negative:
            return Condition(type=ConditionType.NOT, sub_conditions=[base_condition])
        
        return base_condition


def merge_constraints_to_dsl(original_dsl: str, constraints: List[ExtraConstraint]) -> str:
    """
    将约束合并到原始DSL中，支持 add/edit/del 三种操作类型
    支持子查询约束（通过contain/in子句中的节点别名）
    
    使用DSL解析器进行解析和修改，确保语法正确性。
    
    Args:
        original_dsl: 原始DSL代码
        constraints: 额外约束列表（包含类型信息）
    
    Returns:
        合并后的DSL代码
    """
    if not constraints:
        return original_dsl
    
    # 解析原始DSL
    parser = DSLParser(original_dsl)
    root_query = parser.parse()
    
    if not root_query:
        _logger.error("Failed to parse original DSL, falling back to original")
        return original_dsl
    
    # 按节点别名和操作类型分组约束
    from collections import defaultdict
    node_constraints_map = defaultdict(lambda: {'add': [], 'edit': [], 'del': []})
    
    for constraint in constraints:
        if not constraint.constraint_path:
            _logger.warning(f"Constraint has empty constraint_path, skipping: {constraint}")
            continue
        
        # 提取节点别名
        # constraint_path 可以是 "nodeAlias" (类型检查) 或 "nodeAlias.property" (属性检查)
        if '.' in constraint.constraint_path:
            node_alias = constraint.constraint_path.split('.')[0]
        else:
            # 没有 "." 说明是对节点本身的约束（如类型检查：binaryoperation_1 is instanceofExpression）
            node_alias = constraint.constraint_path

        # 检查该alias是否存在（主查询或子查询）
        query = parser.get_node_by_alias(node_alias)
        if query:
            node_constraints_map[node_alias][constraint.constraint_type.value].append(constraint)
        else:
            _logger.warning(f"Node alias '{node_alias}' not found in DSL, skipping constraint: {constraint.constraint_path}")
    
    # 递归处理所有查询（包括嵌套查询）中的约束
    def process_query_constraints(query: Query, node_alias: str, constraint_groups: Dict[str, List[ExtraConstraint]]):
        """处理单个查询的约束"""
        # 先处理 DEL 操作
        for del_constraint in constraint_groups['del']:
            new_condition = remove_condition_by_path(
                query.condition,
                del_constraint.constraint_path,
                del_constraint.operator,
                del_constraint.original_value
            )
            if new_condition is None:
                _logger.warning(f"All conditions removed for node '{node_alias}', keeping minimal condition")
                # 创建一个占位条件以避免空条件
                dummy_attr = Attribute(alias=node_alias, properties=[])
                dummy_vm = ValueMatch(attribute=dummy_attr, operator="is", value=query.entity.node_type)
                dummy_atomic = AtomicCondition(type=AtomicCondType.VALUE_MATCH, value_match=dummy_vm)
                query.condition = Condition(type=ConditionType.ATOMIC, atomic=dummy_atomic)
            else:
                query.condition = new_condition
        
        # 再处理 EDIT 操作
        for edit_constraint in constraint_groups['edit']:
            query.condition = update_condition_by_path(
                query.condition,
                edit_constraint.constraint_path,
                edit_constraint.operator,
                edit_constraint.value,
                edit_constraint.original_value
            )
        
        # 最后处理 ADD 操作
        for add_constraint in constraint_groups['add']:
            new_condition = constraint_to_condition(add_constraint)
            if new_condition:
                query.condition = add_condition_to_tree(query.condition, new_condition, use_and=True)
        
        # 递归处理嵌套查询（在relMatch中）
        def process_nested_queries(cond: Condition):
            """递归处理条件中的嵌套查询"""
            if cond.type == ConditionType.ATOMIC and cond.atomic:
                if cond.atomic.type == AtomicCondType.REL_MATCH and cond.atomic.rel_match:
                    nested_query = cond.atomic.rel_match.query
                    nested_alias = nested_query.entity.alias
                    if nested_alias and nested_alias in node_constraints_map:
                        process_query_constraints(nested_query, nested_alias, node_constraints_map[nested_alias])
            
            # 递归处理子条件
            for sub_cond in cond.sub_conditions:
                process_nested_queries(sub_cond)
        
        process_nested_queries(query.condition)
    
    # 处理每个节点的约束
    for node_alias, constraint_groups in node_constraints_map.items():
        query = parser.get_node_by_alias(node_alias)
        if not query:
            continue
        
        process_query_constraints(query, node_alias, constraint_groups)
    
    # 将修改后的AST转换回DSL
    refined_dsl = query_to_dsl(root_query, is_nested=False)
    
    _logger.info(f"Applied constraints to DSL using AST-based approach")
    return refined_dsl


# 注意：以下旧的基于字符串操作的函数已被基于AST的实现替代
# 保留这些函数仅用于向后兼容，但建议使用 merge_constraints_to_dsl 函数
# 该函数现在使用DSL解析器进行解析和修改，确保语法正确性
