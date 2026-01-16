"""
DSL AST数据结构定义

定义DSL解析后的抽象语法树（AST）数据结构。
这些数据结构被解析器使用，与具体的解析实现（手动或ANTLR）无关。
"""
from typing import List, Optional
from dataclasses import dataclass, field
from enum import Enum


class ConditionType(Enum):
    """条件类型"""
    ATOMIC = "atomic"  # 原子条件
    NOT = "not"  # not (Condition)
    AND = "and"  # and (Condition, Condition, ...)
    OR = "or"  # or (Condition, Condition, ...)


class AtomicCondType(Enum):
    """原子条件类型"""
    VALUE_MATCH = "value_match"  # ValueMatch: Attribute ValueComp Value
    REL_MATCH = "rel_match"  # RelMatch: Attribute NodeComp Query


@dataclass
class Attribute:
    """属性：Alias (.Property)*"""
    alias: str
    properties: List[str] = field(default_factory=list)
    
    def __str__(self) -> str:
        if self.properties:
            return f"{self.alias}.{'.'.join(self.properties)}"
        return self.alias


@dataclass
class ValueMatch:
    """值匹配：Attribute ValueComp Value"""
    attribute: Attribute
    operator: str  # match, is, ==, !=
    value: str  # NodeType | <var name> | literal value


@dataclass
class RelMatch:
    """关系匹配：Attribute NodeComp Query"""
    attribute: Attribute
    operator: str  # contain, in
    query: 'Query'  # 嵌套查询


@dataclass
class AtomicCondition:
    """原子条件：ValueMatch | RelMatch"""
    type: AtomicCondType
    value_match: Optional[ValueMatch] = None
    rel_match: Optional[RelMatch] = None


@dataclass
class Condition:
    """条件：AtomicCond | not (Condition) | and (Condition, ...) | or (Condition, ...)"""
    type: ConditionType
    atomic: Optional[AtomicCondition] = None
    sub_conditions: List['Condition'] = field(default_factory=list)  # 用于 not/and/or


@dataclass
class EntityDecl:
    """实体声明：NodeType (Alias)?"""
    node_type: str
    alias: Optional[str] = None


@dataclass
class Query:
    """查询：EntityDecl where Condition ;"""
    entity: EntityDecl
    condition: Condition
    # 位置信息（用于后续修改）
    start_pos: int = 0  # 查询开始位置
    end_pos: int = 0  # 查询结束位置（分号后）
    where_start: int = 0  # where 关键字开始位置
    where_end: int = 0  # where 子句结束位置（分号后）
    condition_start: int = 0  # 条件开始位置
    condition_end: int = 0  # 条件结束位置（分号前）
