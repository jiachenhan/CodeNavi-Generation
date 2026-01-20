"""
基于ANTLR的DSL语法解析器
"""
from typing import List, Dict, Optional, Any
from dataclasses import dataclass, field
from enum import Enum
from pathlib import Path

# 导入ANTLR生成的解析器（必需）
# 注意：需要先运行 antlr4 -Dlanguage=Python3 DSL.g4 生成解析器
# 生成的文件应该在 app/refine/parser/antlr_generated/ 目录下
import sys
antlr_gen_path = Path(__file__).parent / 'antlr_generated'
if not antlr_gen_path.exists():
    raise RuntimeError(
        f"ANTLR generated parser not found at {antlr_gen_path}. "
        "Please run generate_antlr_parser.bat (Windows) or generate_antlr_parser.sh (Linux/macOS) "
        "to generate the parser from DSL.g4"
    )

sys.path.insert(0, str(antlr_gen_path))
from DSLLexer import DSLLexer
from DSLParser import DSLParser as AntlrDSLParser
from DSLListener import DSLListener
from DSLVisitor import DSLVisitor
from antlr4 import InputStream, CommonTokenStream, Recognizer, Token, RecognitionException

from app.refine.parser.dsl_ast import (
    ConditionType, AtomicCondType, Attribute, ValueMatch, RelMatch,
    AtomicCondition, Condition, EntityDecl, Query
)
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


class DSLParserANTLR:
    """
    基于ANTLR的DSL解析器
    
    使用ANTLR4生成的解析器，提供与DSLParser相同的接口。
    优势：
    1. 更准确的语法分析
    2. 详细的错误信息和位置
    3. 更好的错误恢复
    4. 易于维护和扩展
    """
    
    def __init__(self, dsl: str):
        """
        初始化ANTLR解析器
        
        Args:
            dsl: DSL代码字符串
        """

        self.dsl = dsl
        self.queries: List[Query] = []
        self.node_map: Dict[str, Query] = {}
        self._error_listener: Optional['DSLParseErrorListener'] = None
    
    def parse(self) -> Optional[Query]:
        """
        解析DSL，返回根查询
        
        Returns:
            根查询对象，如果解析失败则返回None
        """
        # 创建词法分析器
        lexer = DSLLexer(InputStream(self.dsl))
        # 创建token流
        token_stream = CommonTokenStream(lexer)
        
        # 创建语法分析器
        parser = AntlrDSLParser(token_stream)
        
        # 添加错误监听器以收集错误信息
        error_listener = DSLParseErrorListener()
        self._error_listener = error_listener  # 保存引用以便后续访问
        parser.removeErrorListeners()
        parser.addErrorListener(error_listener)
        
        # 解析（ANTLR默认不会抛出异常，错误通过监听器收集）
        tree = parser.query()
        
        # 检查是否有错误（错误信息已由监听器收集）
        if error_listener.errors:
            for error in error_listener.errors:
                _logger.error(f"Parse error: {error}")
            return None
        
        # 使用Visitor模式构建AST
        visitor = DSLToASTVisitor()
        root_query = visitor.visit(tree)
        
        if root_query:
            self.queries = visitor.all_queries
            self.node_map = visitor.node_map
            # _logger.debug(f"Parsed DSL with {len(self.queries)} queries, {len(self.node_map)} named nodes")
        return root_query
    
    def get_all_queries(self) -> List[Query]:
        """获取所有解析的查询（包括嵌套查询）"""
        return self.queries
    
    def get_node_by_alias(self, alias: str) -> Optional[Query]:
        """根据别名获取查询节点"""
        return self.node_map.get(alias)
    
    def get_parse_errors(self) -> 'DSLParseErrorListener':
        """
        获取解析错误监听器（包含所有错误信息）
        
        返回错误监听器实例，可以直接访问错误信息。
        如果还没有创建监听器，返回一个空的监听器实例。
        """
        if self._error_listener is None:
            _logger.error("No error listener found")
            return DSLParseErrorListener()
        return self._error_listener
    
    def get_parse_errors_as_strings(self) -> List[str]:
        """获取解析错误列表（字符串格式，向后兼容）"""
        if self._error_listener is None:
            _logger.error("No error listener found")
            return []
        return [str(error) for error in self._error_listener.errors]
    
    def get_error_position(self) -> Optional[int]:
        """获取第一个错误的位置（字符索引）"""
        if self._error_listener is None:
            _logger.error("No error listener found")
            return None
        return self._error_listener.get_first_error_position()

    def parse_condition(self, node_map: Dict[str, Query] = None) -> Optional[Condition]:
        """
        解析独立的Condition（不需要完整的Query结构）

        Args:
            node_map: 可选的节点别名映射，用于验证时的上下文信息

        Returns:
            Condition AST对象，如果解析失败则返回None
        """
        # 创建词法分析器
        lexer = DSLLexer(InputStream(self.dsl))
        # 创建token流
        token_stream = CommonTokenStream(lexer)

        # 创建语法分析器
        parser = AntlrDSLParser(token_stream)

        # 添加错误监听器
        error_listener = DSLParseErrorListener()
        self._error_listener = error_listener
        parser.removeErrorListeners()
        parser.addErrorListener(error_listener)

        # 直接解析 condition 规则
        tree = parser.condition()

        # 检查是否有错误
        if error_listener.errors:
            for error in error_listener.errors:
                _logger.error(f"Parse error in condition: {error}")
            return None

        # 使用Visitor模式构建AST
        visitor = DSLToASTVisitor()
        if node_map:
            visitor.node_map = node_map  # 传入已有的node_map用于验证
        condition = visitor.visit(tree)

        return condition
    

class ParseErrorType(Enum):
    """解析错误类型"""
    SYNTAX_ERROR = "syntax_error"  # 语法错误
    RECOGNITION_ERROR = "recognition_error"  # 识别错误
    UNEXPECTED_ERROR = "unexpected_error"  # 意外错误


@dataclass
class ParseError:
    """解析错误信息封装"""
    error_type: ParseErrorType
    message: str
    line: Optional[int] = None
    column: Optional[int] = None
    position: Optional[int] = None  # 字符索引位置
    token: Optional[str] = None  # 出错的token
    
    def __str__(self) -> str:
        """转换为字符串表示"""
        parts = [f"[{self.error_type.value}]"]
        if self.line is not None and self.column is not None:
            parts.append(f"Line {self.line}, column {self.column}")
        if self.position is not None:
            parts.append(f"Position {self.position}")
        parts.append(self.message)
        if self.token:
            parts.append(f"(token: {self.token})")
        return ": ".join(parts)
    
    def to_dict(self) -> Dict:
        """转换为字典格式"""
        return {
            "error_type": self.error_type.value,
            "message": self.message,
            "line": self.line,
            "column": self.column,
            "position": self.position,
            "token": self.token
        }


class DSLParseErrorListener:
    """
    ANTLR错误监听器，收集解析错误
    
    这个类实现了ANTLR的ErrorListener接口，用于在解析过程中收集语法错误。
    错误信息会被收集到self.errors列表中。
    
    注意：这个监听器实例会被保存在DSLParserANTLR._error_listener中，
    以便在解析完成后访问错误信息。这是ANTLR要求的错误收集机制。
    """
    
    def __init__(self):
        self.errors: List[ParseError] = []
    
    def has_errors(self) -> bool:
        """是否有错误"""
        return len(self.errors) > 0
    
    def get_first_error(self) -> Optional[ParseError]:
        """获取第一个错误"""
        return self.errors[0] if self.errors else None
    
    def get_first_error_position(self) -> Optional[int]:
        """获取第一个错误的位置（字符索引）"""
        first = self.get_first_error()
        return first.position if first else None
    
    def to_string_list(self) -> List[str]:
        """转换为字符串列表（向后兼容）"""
        return [str(error) for error in self.errors]
    
    def __len__(self) -> int:
        """错误数量"""
        return len(self.errors)
    
    def __iter__(self):
        """迭代错误"""
        return iter(self.errors)
    
    def syntaxError(
        self,
        recognizer: Recognizer,
        offendingSymbol: Optional[Token],
        line: int,
        column: int,
        msg: str,
        e: Optional[RecognitionException]
    ) -> None:
        """
        语法错误回调
        
        当ANTLR解析器遇到语法错误时，会调用这个方法。
        错误信息会被封装成ParseError对象并添加到errors列表中。
        
        Args:
            recognizer: ANTLR识别器（Parser或Lexer）
            offendingSymbol: 出错的符号（Token），可能为None
            line: 错误所在行号（从1开始）
            column: 错误所在列号（从0开始）
            msg: 错误消息
            e: 识别异常（RecognitionException），可能为None
        """
        position = offendingSymbol.start if offendingSymbol else None
        token = offendingSymbol.text if offendingSymbol else None
        
        error = ParseError(
            error_type=ParseErrorType.SYNTAX_ERROR,
            message=msg,
            line=line,
            column=column,
            position=position,
            token=token
        )
        
        self.errors.append(error)
    
    def reportAmbiguity(
        self,
        recognizer: Recognizer,
        dfa: Any,
        startIndex: int,
        stopIndex: int,
        exact: bool,
        ambigAlts: set,
        configs: Any
    ) -> None:
        """
        报告歧义
        
        ANTLR在解析时遇到歧义时会调用此方法。
        对于我们的用例，通常不需要特殊处理，但必须实现此方法。
        """
        # 对于DSL解析，我们通常不需要处理歧义报告
        # 但保留此方法以满足ErrorListener接口要求
        pass
    
    def reportAttemptingFullContext(
        self,
        recognizer: Recognizer,
        dfa: Any,
        startIndex: int,
        stopIndex: int,
        conflictingAlts: set,
        configs: Any
    ) -> None:
        """
        报告尝试完整上下文
        
        ANTLR在尝试完整上下文解析时会调用此方法。
        对于我们的用例，通常不需要特殊处理，但必须实现此方法。
        """
        # 对于DSL解析，我们通常不需要处理完整上下文报告
        # 但保留此方法以满足ErrorListener接口要求
        pass
    
    def reportContextSensitivity(
        self,
        recognizer: Recognizer,
        dfa: Any,
        startIndex: int,
        stopIndex: int,
        prediction: int,
        configs: Any
    ) -> None:
        """
        报告上下文敏感性
        
        ANTLR在检测到上下文敏感性时会调用此方法。
        对于我们的用例，通常不需要特殊处理，但必须实现此方法。
        """
        # 对于DSL解析，我们通常不需要处理上下文敏感性报告
        # 但保留此方法以满足ErrorListener接口要求
        pass
    

# 定义Visitor类
class DSLToASTVisitor(DSLVisitor):
    """
    ANTLR Visitor，将ANTLR语法树转换为我们的AST结构
    """
    
    def __init__(self):
        super().__init__()
        self.all_queries: List[Query] = []
        self.node_map: Dict[str, Query] = {}
        self._query_stack: List[Query] = []  # 用于处理嵌套查询
    
    def visitQuery(self, ctx: AntlrDSLParser.QueryContext) -> Optional[Query]:
        """访问查询节点"""
        # 解析实体声明
        entity_ctx = ctx.entityDecl()
        node_type = entity_ctx.nodeType().IDENTIFIER().getText()
        alias_ctx = entity_ctx.alias()
        alias = alias_ctx.IDENTIFIER().getText() if alias_ctx else None
        
        entity = EntityDecl(node_type=node_type, alias=alias)
        
        # 解析条件
        condition = self.visit(ctx.condition())
        if not condition:
            return None
        
        # 创建查询对象
        query = Query(
            entity=entity,
            condition=condition,
            start_pos=ctx.start.start,
            end_pos=ctx.stop.stop + 1,
            where_start=ctx.WHERE().symbol.start,
            where_end=ctx.SEMICOLON().symbol.stop + 1,
            condition_start=ctx.condition().start.start,
            condition_end=ctx.condition().stop.stop + 1
        )
        
        # 记录查询
        self.all_queries.append(query)
        if alias:
            self.node_map[alias] = query
        
        return query
    
    def visitCondition(self, ctx: AntlrDSLParser.ConditionContext) -> Optional[Condition]:
        """访问条件节点"""
        # 原子条件
        if ctx.atomicCondition():
            atomic = self.visit(ctx.atomicCondition())
            if atomic:
                return Condition(type=ConditionType.ATOMIC, atomic=atomic)
        
        # not (Condition)
        if ctx.NOT():
            sub_condition = self.visit(ctx.condition(0))
            if sub_condition:
                return Condition(type=ConditionType.NOT, sub_conditions=[sub_condition])
        
        # and (Condition, ...)
        if ctx.AND():
            sub_conditions = [self.visit(c) for c in ctx.condition()]
            sub_conditions = [c for c in sub_conditions if c is not None]
            if sub_conditions:
                return Condition(type=ConditionType.AND, sub_conditions=sub_conditions)
        
        # or (Condition, ...)
        if ctx.OR():
            sub_conditions = [self.visit(c) for c in ctx.condition()]
            sub_conditions = [c for c in sub_conditions if c is not None]
            if sub_conditions:
                return Condition(type=ConditionType.OR, sub_conditions=sub_conditions)
        
        return None
    
    def visitAtomicCondition(self, ctx: AntlrDSLParser.AtomicConditionContext) -> Optional[AtomicCondition]:
        """访问原子条件节点"""
        if ctx.valueMatch():
            value_match = self.visit(ctx.valueMatch())
            if value_match:
                return AtomicCondition(type=AtomicCondType.VALUE_MATCH, value_match=value_match)
        
        if ctx.relMatch():
            rel_match = self.visit(ctx.relMatch())
            if rel_match:
                return AtomicCondition(type=AtomicCondType.REL_MATCH, rel_match=rel_match)
        
        # 布尔属性（仅属性，隐式检查为true）
        if ctx.attributeOnly():
            attribute = self.visit(ctx.attributeOnly().attribute())
            if attribute:
                # 将布尔属性转换为 valueMatch: attribute == true
                dummy_value_match = ValueMatch(
                    attribute=attribute,
                    operator="==",
                    value="true"
                )
                return AtomicCondition(type=AtomicCondType.VALUE_MATCH, value_match=dummy_value_match)
        
        return None
    
    def visitValueMatch(self, ctx: AntlrDSLParser.ValueMatchContext) -> Optional[ValueMatch]:
        """访问值匹配节点"""
        attribute = self.visit(ctx.attribute())
        if not attribute:
            return None
        
        # 解析操作符
        value_comp_ctx = ctx.valueComp()
        if value_comp_ctx.MATCH():
            operator = "match"
        elif value_comp_ctx.IS():
            operator = "is"
        elif value_comp_ctx.EQ():
            operator = "=="
        elif value_comp_ctx.NE():
            operator = "!="
        else:
            return None
        
        # 解析值
        value_ctx = ctx.value()
        if value_ctx.stringLiteral():
            value = value_ctx.stringLiteral().getText()
        elif value_ctx.numberLiteral():
            value = value_ctx.numberLiteral().getText()
        elif value_ctx.booleanLiteral():
            value = value_ctx.booleanLiteral().getText()
        elif value_ctx.identifier():
            value = value_ctx.identifier().IDENTIFIER().getText()
        elif value_ctx.nodeType():
            value = value_ctx.nodeType().IDENTIFIER().getText()
        else:
            return None
        
        return ValueMatch(attribute=attribute, operator=operator, value=value)
    
    def visitRelMatch(self, ctx: AntlrDSLParser.RelMatchContext) -> Optional[RelMatch]:
        """访问关系匹配节点"""
        attribute = self.visit(ctx.attribute())
        if not attribute:
            return None
        
        # 解析操作符
        node_comp_ctx = ctx.nodeComp()
        if node_comp_ctx.CONTAIN():
            operator = "contain"
        elif node_comp_ctx.IN():
            operator = "in"
        elif node_comp_ctx.NOT_IN():
            operator = "notIn"
        else:
            return None
        
        # 解析嵌套查询
        nested_query_ctx = ctx.nestedQuery()
        if nested_query_ctx:
            query = self.visit(nested_query_ctx)
        else:
            return None
        
        if not query:
            return None
        
        return RelMatch(attribute=attribute, operator=operator, query=query)
    
    def visitNestedQuery(self, ctx: AntlrDSLParser.NestedQueryContext) -> Optional[Query]:
        """访问嵌套查询节点"""
        # 解析实体声明
        entity_ctx = ctx.entityDecl()
        node_type = entity_ctx.nodeType().IDENTIFIER().getText()
        alias_ctx = entity_ctx.alias()
        alias = alias_ctx.IDENTIFIER().getText() if alias_ctx else None
        
        entity = EntityDecl(node_type=node_type, alias=alias)
        
        # 解析条件（可选）
        condition = None
        # condition() 方法在嵌套查询中是可选的，如果不存在会返回None
        condition_ctx = ctx.condition()
        if condition_ctx:
            condition = self.visit(condition_ctx)
        
        # 如果没有条件，创建一个占位条件
        if condition is None:
            dummy_attribute = Attribute(alias=alias or node_type, properties=[])
            dummy_value_match = ValueMatch(
                attribute=dummy_attribute,
                operator="is",
                value=node_type
            )
            dummy_atomic = AtomicCondition(
                type=AtomicCondType.VALUE_MATCH,
                value_match=dummy_value_match
            )
            condition = Condition(
                type=ConditionType.ATOMIC,
                atomic=dummy_atomic
            )
        
        # 确定结束位置（可能是分号、逗号或右括号）
        end_token = ctx.stop if ctx.stop else ctx.start
        where_start = ctx.WHERE().symbol.start if ctx.WHERE() else ctx.start.start
        where_end = end_token.stop + 1
        
        # 创建查询对象
        query = Query(
            entity=entity,
            condition=condition,
            start_pos=ctx.start.start,
            end_pos=where_end,
            where_start=where_start if ctx.WHERE() else ctx.start.start,
            where_end=where_end,
            condition_start=condition_ctx.start.start if condition_ctx else ctx.start.start,
            condition_end=condition_ctx.stop.stop + 1 if condition_ctx else ctx.start.start
        )
        
        # 记录查询
        self.all_queries.append(query)
        if alias:
            self.node_map[alias] = query
        
        return query
    
    def visitAttribute(self, ctx: AntlrDSLParser.AttributeContext) -> Optional[Attribute]:
        """访问属性节点"""
        alias_ctx = ctx.alias()
        if not alias_ctx:
            return None
        
        alias = alias_ctx.IDENTIFIER().getText()
        properties = [prop.IDENTIFIER().getText() for prop in ctx.propertyName()]
        
        return Attribute(alias=alias, properties=properties)
    
    def visitEntityDecl(self, ctx: AntlrDSLParser.EntityDeclContext) -> EntityDecl:
        """访问实体声明节点"""
        node_type = ctx.nodeType().IDENTIFIER().getText()
        alias_ctx = ctx.alias()
        alias = alias_ctx.IDENTIFIER().getText() if alias_ctx else None
        
        return EntityDecl(node_type=node_type, alias=alias)
