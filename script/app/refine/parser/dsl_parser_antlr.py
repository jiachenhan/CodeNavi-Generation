"""
基于ANTLR的DSL语法解析器

使用ANTLR4生成的解析器，提供更准确的语法分析和错误处理。
与现有的DSLParser接口兼容，可以逐步替换。
"""
from typing import List, Dict, Optional
from dataclasses import dataclass, field
from enum import Enum
import sys
import os

# 尝试导入ANTLR生成的解析器
try:
    # 注意：需要先运行 antlr4 -Dlanguage=Python3 DSL.g4 生成解析器
    # 生成的文件应该在 app/refine/parser/antlr_generated/ 目录下
    antlr_gen_path = os.path.join(os.path.dirname(__file__), 'antlr_generated')
    if os.path.exists(antlr_gen_path):
        sys.path.insert(0, antlr_gen_path)
    from DSLLexer import DSLLexer
    from DSLParser import DSLParser as AntlrDSLParser
    from DSLListener import DSLListener
    from DSLVisitor import DSLVisitor
    from antlr4 import InputStream, CommonTokenStream, RecognitionException
    ANTLR_AVAILABLE = True
except ImportError:
    ANTLR_AVAILABLE = False
    # 延迟导入logger，避免在导入时出错
    pass

from app.refine.parser.dsl_ast import (
    ConditionType, AtomicCondType, Attribute, ValueMatch, RelMatch,
    AtomicCondition, Condition, EntityDecl, Query
)
from app.refine.parser.dsl_validator import DSLValidator, ValidationResult
from app.refine.parser.dsl_fix_suggester import DSLFixSuggester
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
        if not ANTLR_AVAILABLE:
            _logger.error("ANTLR runtime not available. Please install antlr4-python3-runtime and generate parser from DSL.g4")
            raise RuntimeError(
                "ANTLR parser not available. Please:\n"
                "1. Install antlr4-python3-runtime: pip install antlr4-python3-runtime\n"
                "2. Generate parser: antlr4 -Dlanguage=Python3 -o antlr_generated DSL.g4\n"
                "3. Ensure antlr_generated directory contains generated files"
            )
        
        self.dsl = dsl
        self.queries: List[Query] = []
        self.node_map: Dict[str, Query] = {}
        self._parse_errors: List[str] = []
        self._error_position: Optional[int] = None
        self._error_listener: Optional['DSLParseErrorListener'] = None
        self._validation_result: Optional[ValidationResult] = None
    
    def parse(self) -> Optional[Query]:
        """
        解析DSL，返回根查询
        
        Returns:
            根查询对象，如果解析失败则返回None
        """
        try:
            # 创建ANTLR输入流
            input_stream = InputStream(self.dsl)
            
            # 创建词法分析器
            lexer = DSLLexer(input_stream)
            
            # 创建token流
            token_stream = CommonTokenStream(lexer)
            
            # 创建语法分析器
            parser = AntlrDSLParser(token_stream)
            
            # 添加错误监听器以收集错误信息
            error_listener = DSLParseErrorListener()
            self._error_listener = error_listener
            parser.removeErrorListeners()
            parser.addErrorListener(error_listener)
            
            # 解析
            tree = parser.query()
            
            # 检查是否有错误
            if error_listener.errors:
                self._parse_errors = error_listener.errors
                self._error_position = error_listener.error_position
                for error in error_listener.errors:
                    _logger.error(f"Parse error: {error}")
                return None
            
            # 使用Visitor模式构建AST
            visitor = DSLToASTVisitor()
            root_query = visitor.visit(tree)
            
            if root_query:
                self.queries = visitor.all_queries
                self.node_map = visitor.node_map
                _logger.debug(f"Parsed DSL with {len(self.queries)} queries, {len(self.node_map)} named nodes")
                
                # 进行语义验证
                validator = DSLValidator()
                self._validation_result = validator.validate(root_query)
                
                if not self._validation_result.is_valid:
                    for error in self._validation_result.errors:
                        _logger.warning(f"Validation error: {error.message}")
                        if error.suggestion:
                            _logger.info(f"  Suggestion: {error.suggestion}")
                    for warning in self._validation_result.warnings:
                        _logger.warning(f"Validation warning: {warning}")
            
            return root_query
            
        except RecognitionException as e:
            error_position = e.offendingToken.start if e.offendingToken else None
            error_msg = f"Recognition error at position {error_position}: {e.getMessage()}"
            self._parse_errors.append(error_msg)
            self._error_position = error_position
            _logger.error(error_msg)
            return None
        except Exception as e:
            error_msg = f"Unexpected error during parsing: {str(e)}"
            self._parse_errors.append(error_msg)
            _logger.error(error_msg, exc_info=True)
            return None
    
    def get_all_queries(self) -> List[Query]:
        """获取所有解析的查询（包括嵌套查询）"""
        return self.queries
    
    def get_node_by_alias(self, alias: str) -> Optional[Query]:
        """根据别名获取查询节点"""
        return self.node_map.get(alias)
    
    def get_parse_errors(self) -> List[str]:
        """获取解析错误列表"""
        return self._parse_errors
    
    def get_error_position(self) -> Optional[int]:
        """获取错误位置（字符索引）"""
        return self._error_position
    
    def get_validation_result(self) -> Optional[ValidationResult]:
        """获取语义验证结果"""
        return self._validation_result
    
    def get_fix_suggestions(self) -> str:
        """获取修复建议消息"""
        if self._validation_result and self._validation_result.errors:
            return DSLFixSuggester.generate_fix_message(self._validation_result.errors)
        return ""


class DSLParseErrorListener:
    """ANTLR错误监听器，收集解析错误"""
    
    def __init__(self):
        self.errors: List[str] = []
        self.error_position: Optional[int] = None
    
    def syntaxError(self, recognizer, offendingSymbol, line, column, msg, e):
        """语法错误回调"""
        error_msg = f"Line {line}, column {column}: {msg}"
        if offendingSymbol:
            error_msg += f" (token: {offendingSymbol.text})"
            # 保存错误位置（字符索引）
            if self.error_position is None:
                self.error_position = offendingSymbol.start
        self.errors.append(error_msg)
    
    def reportAmbiguity(self, recognizer, dfa, startIndex, stopIndex, exact, ambigAlts, configs):
        """歧义报告（可选）"""
        pass
    
    def reportAttemptingFullContext(self, recognizer, dfa, startIndex, stopIndex, conflictingAlts, configs):
        """全上下文尝试报告（可选）"""
        pass
    
    def reportContextSensitivity(self, recognizer, dfa, startIndex, stopIndex, prediction, configs):
        """上下文敏感性报告（可选）"""
        pass


# 定义Visitor基类
if ANTLR_AVAILABLE:
    class DSLToASTVisitor(DSLVisitor):
        """
        ANTLR Visitor，将ANTLR语法树转换为我们的AST结构
        """
        
        def __init__(self):
            super().__init__()
            self.all_queries: List[Query] = []
            self.node_map: Dict[str, Query] = {}
            self._query_stack: List[Query] = []  # 用于处理嵌套查询
        
        def visitQuery(self, ctx):
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
        
        def visitCondition(self, ctx):
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
        
        def visitAtomicCondition(self, ctx):
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
        
        def visitValueMatch(self, ctx):
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
        
        def visitRelMatch(self, ctx):
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
        
        def visitNestedQuery(self, ctx):
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
        
        def visitAttribute(self, ctx):
            """访问属性节点"""
            alias_ctx = ctx.alias()
            if not alias_ctx:
                return None
            
            alias = alias_ctx.IDENTIFIER().getText()
            properties = [prop.IDENTIFIER().getText() for prop in ctx.propertyName()]
            
            return Attribute(alias=alias, properties=properties)
        
        def visitEntityDecl(self, ctx):
            """访问实体声明节点"""
            node_type = ctx.nodeType().IDENTIFIER().getText()
            alias_ctx = ctx.alias()
            alias = alias_ctx.IDENTIFIER().getText() if alias_ctx else None
            
            return EntityDecl(node_type=node_type, alias=alias)
else:
    # 占位类，避免在ANTLR不可用时出错
    class DSLToASTVisitor:
        def __init__(self):
            self.all_queries: List[Query] = []
            self.node_map: Dict[str, Query] = {}
            self._query_stack: List[Query] = []
