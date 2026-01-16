"""
测试DSL验证器

测试用例：
1. 测试无效的节点类型
2. 测试不支持的属性路径（如 binaryoperation_1.operator）
3. 测试属性值修复建议
"""
from app.refine.parser.dsl_parser_antlr import DSLParserANTLR
from app.refine.parser.dsl_validator import DSLValidator
from app.refine.parser.dsl_fix_suggester import DSLFixSuggester


def test_unsupported_operator_property():
    """测试不支持的operator属性"""
    # 错误的DSL：binaryoperation_1.operator is instanceof
    dsl = "binaryOperation binaryoperation_1 where binaryoperation_1.operator is instanceof ;"
    
    parser = DSLParserANTLR(dsl)
    query = parser.parse()
    
    if query:
        validator = DSLValidator()
        result = validator.validate(query)
        
        print("=== Test: Unsupported operator property ===")
        print(f"DSL: {dsl}")
        print(f"Valid: {result.is_valid}")
        print(f"Errors: {len(result.errors)}")
        
        for error in result.errors:
            print(f"  - {error.message}")
            if error.suggestion:
                print(f"    Suggestion: {error.suggestion}")
        
        # 生成修复建议
        if result.errors:
            fix_message = DSLFixSuggester.generate_fix_message(result.errors)
            print("\nFix suggestions:")
            print(fix_message)
    else:
        print("Parse failed:")
        for error in parser.get_parse_errors():
            print(f"  - {error}")


def test_invalid_node_type():
    """测试无效的节点类型"""
    dsl = "invalidNodeType node1 where node1.name == \"test\" ;"
    
    parser = DSLParserANTLR(dsl)
    query = parser.parse()
    
    if query:
        validator = DSLValidator()
        result = validator.validate(query)
        
        print("\n=== Test: Invalid node type ===")
        print(f"DSL: {dsl}")
        print(f"Valid: {result.is_valid}")
        
        for error in result.errors:
            print(f"  - {error.message}")
            if error.suggestion:
                print(f"    Suggestion: {error.suggestion}")


def test_correct_dsl():
    """测试正确的DSL"""
    # 正确的DSL：binaryoperation_1 is instanceofExpression
    dsl = "binaryOperation binaryoperation_1 where binaryoperation_1 is instanceofExpression ;"
    
    parser = DSLParserANTLR(dsl)
    query = parser.parse()
    
    if query:
        validator = DSLValidator()
        result = validator.validate(query)
        
        print("\n=== Test: Correct DSL ===")
        print(f"DSL: {dsl}")
        print(f"Valid: {result.is_valid}")
        print(f"Errors: {len(result.errors)}")
        print(f"Warnings: {len(result.warnings)}")


if __name__ == "__main__":
    test_unsupported_operator_property()
    test_invalid_node_type()
    test_correct_dsl()
