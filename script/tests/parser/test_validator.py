"""
测试 DSL 验证器

使用 pytest 框架重写的测试。
"""
import pytest

# antlr4 是必需的依赖，直接导入（如果失败会在导入时报错）
from app.refine.parser.dsl_parser_antlr import DSLParserANTLR
from app.refine.parser.dsl_validator import DSLValidator
from app.refine.parser.dsl_fix_suggester import DSLFixSuggester


class TestDSLValidator:
    """DSL 验证器测试类"""
    
    def test_unsupported_operator_property(self):
        """测试不支持的 operator 属性"""
        # 错误的 DSL：binaryoperation_1.operator is instanceof
        dsl = "binaryOperation binaryoperation_1 where binaryoperation_1.operator is instanceof ;"
        
        parser = DSLParserANTLR(dsl)
        query = parser.parse()
        
        assert query is not None, "DSL should parse successfully"
        
        validator = DSLValidator()
        result = validator.validate(query)
        
        assert not result.is_valid, "DSL with unsupported operator property should be invalid"
        assert len(result.errors) > 0, "Should have validation errors"
        
        # 检查错误消息
        error_messages = [error.message for error in result.errors]
        assert any("operator" in msg.lower() or "unsupported" in msg.lower() for msg in error_messages), \
            "Error message should mention operator or unsupported"
        
        # 生成修复建议
        if result.errors:
            fix_message = DSLFixSuggester.generate_fix_message(result.errors)
            assert len(fix_message) > 0, "Fix suggestions should be generated"
    
    def test_invalid_node_type(self):
        """测试无效的节点类型"""
        dsl = "invalidNodeType node1 where node1.name == \"test\" ;"
        
        parser = DSLParserANTLR(dsl)
        query = parser.parse()
        
        assert query is not None, "DSL should parse successfully"
        
        validator = DSLValidator()
        result = validator.validate(query)
        
        assert not result.is_valid, "DSL with invalid node type should be invalid"
        assert len(result.errors) > 0, "Should have validation errors"
        
        # 检查错误消息
        error_messages = [error.message for error in result.errors]
        assert any("invalid" in msg.lower() and "node" in msg.lower() for msg in error_messages), \
            "Error message should mention invalid node type"
    
    def test_correct_dsl(self):
        """测试正确的 DSL"""
        # 正确的 DSL：binaryoperation_1 is instanceofExpression
        dsl = "binaryOperation binaryoperation_1 where binaryoperation_1 is instanceofExpression ;"
        
        parser = DSLParserANTLR(dsl)
        query = parser.parse()
        
        assert query is not None, "DSL should parse successfully"
        
        validator = DSLValidator()
        result = validator.validate(query)
        
        # 正确的 DSL 应该通过验证（或者只有警告）
        # 注意：验证器可能会报告警告，但不应该有错误
        assert result.is_valid or len(result.errors) == 0, \
            "Correct DSL should have no validation errors"
