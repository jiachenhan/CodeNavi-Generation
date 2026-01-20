"""
DSL元数据定义

从Java代码中提取的节点类型和属性映射关系。
这些数据用于验证DSL的语义正确性。

数据来源：
- DSLNodeMapping.java: 节点类型映射
- DSLRoleMapping.java: 属性（role）映射
- 各个DSLNode类的prettyPrint()方法: 节点类型名称
- 各个DSLRole类的prettyPrint()方法: 属性名称
"""

# 有效的节点类型（从DSLNode类的prettyPrint()方法提取）
VALID_NODE_TYPES = {
    "binaryOperation",
    "unaryOperation",
    "valueDeclaration",
    "thisExpression",
    "throwStatement",
    "tryWithResources",
    "tryBlock",
    "switchBlock",
    "synchronizedBlock",
    "ternaryOperation",
    "thisCall",
    "returnStatement",
    "superCall",
    "nullLiteral",
    "objectCreationExpression",
    "lambdaExpression",
    "literal",
    "methodReferenceExpression",
    "forBlock",
    "functionCall",
    "functionDeclaration",
    "ifBlock",
    "initArrayExpression",
    "instanceofExpression",
    "doWhileBlock",
    "exceptionBlock",
    "fieldAccess",
    "forEachBlock",
    "finallyBlock",
    "defaultStatement",
    "continueStatement",
    "catchBlock",
    "castExpression",
    "assertStatement",
    "caseStatement",
    "breakStatement",
    "anonymousInnerClassExpression",
    "arrayCreationExpression",
    "arrayAccess",
    "annotation",
    "annoMember",
    "whileBlock",
}

# 有效的属性名称（从DSLRole类的prettyPrint()方法提取）
VALID_PROPERTIES = {
    "body",
    "arguments",
    "type",
    "condition",
    "returnValue",
    "operand",
    "lhs",
    "rhs",
    "name",
    "value",
    "parameters",
    "base",
    "baseType",
    "castType",
    "arrayIndex",
    "dimensions",
    "initArray",
    "elements",
    "thenBlock",
    "elseBlock",
    "thenExpression",
    "elseExpression",
    "tryBlock",
    "catchBlocks",
    "finallyBlock",
    "tryResources",
    "forInit",
    "initialization",
    "iteration",
    "forIter",
    "switchSelector",
    "selector",
    "caseExpression",
    "variable",
    "iterable",
    "forEachVariable",
    "forEachIterable",
    "lock",
    "exceptionTypes",
    "throwExceptionTypes",
    "annotations",
    "annoValue",
    "annoMembers",
    "anonymousClassBody",
    "generics",
    "initializer",
    "variableInit",
    "message",
    "assertMessage",
    "primitiveTypeCode",
}

# 属性的RoleAction类型分类（从DSLRole子类提取）
# 这决定了属性如何被访问
PROPERTY_ROLE_ACTIONS = {
    # Collection类型: 包含多个节点,必须使用contain/in查询,不能直接用"."访问子属性
    "arguments": "Collection",
    "parameters": "Collection",
    "dimensions": "Collection",
    "elements": "Collection",  # arrayInitializer.elements
    "generics": "Collection",
    "throwExceptionTypes": "Collection",

    # Body类型: 包含多个语句,必须使用contain查询
    "body": "Body",
    "thenBlock": "Body",
    "elseBlock": "Body",
    "tryBlock": "Body",
    "catchBlocks": "Body",
    "finallyBlock": "Body",
    "tryResources": "Body",
    "annoMembers": "Body",
    "annotations": "Body",
    "anonymousClassBody": "Body",

    # Child类型: 单个子节点,可以用"."访问其属性
    "type": "Child",
    "condition": "Child",
    "returnValue": "Child",
    "operand": "Child",
    "lhs": "Child",
    "rhs": "Child",
    "base": "Child",
    "baseType": "Child",
    "castType": "Child",
    "arrayIndex": "Child",
    "initArray": "Child",
    "thenExpression": "Child",
    "elseExpression": "Child",
    "forInit": "Child",
    "forIter": "Child",
    "switchSelector": "Child",
    "caseExpression": "Child",
    "forEachVariable": "Child",
    "forEachIterable": "Child",
    "lock": "Child",
    "annoValue": "Child",
    "variableInit": "Child",
    "assertMessage": "Child",

    # Simple类型: 简单值,可以直接比较
    "value": "Simple",  # 字面量的值
    "primitiveTypeCode": "Simple",
}

# 节点类型到有效属性的映射
# 注意：这个映射需要根据DSLRoleMapping.java中的实际映射关系来构建
# 这里先提供一个基础版本，后续可以根据需要扩展
NODE_TYPE_TO_PROPERTIES = {
    "binaryOperation": {"lhs", "rhs"},  # operator属性被映射为SkipRole，不支持
    "unaryOperation": {"operand"},
    "valueDeclaration": {"type", "initializer"},
    "functionDeclaration": {"returnType", "parameters", "body", "exceptionTypes"},
    "functionCall": {"arguments"},
    "ifBlock": {"condition", "thenBlock", "elseBlock"},
    "whileBlock": {"condition", "body"},
    "forBlock": {"initialization", "condition", "iteration", "body"},
    "forEachBlock": {"variable", "iterable", "body"},
    "switchBlock": {"selector", "body"},
    "caseStatement": {"caseExpression"},
    "tryBlock": {"tryBlock", "catchBlocks", "finallyBlock"},
    "exceptionBlock": {"tryBlock", "catchBlocks", "finallyBlock"},
    "catchBlock": {"parameters", "body"},
    "throwStatement": {"operand"},
    "returnStatement": {"returnValue"},
    "arrayAccess": {"base", "arrayIndex"},
    "arrayCreationExpression": {"dimensions", "initArray"},
    "initArrayExpression": {"elements"},
    "castExpression": {"castType", "operand"},
    "fieldAccess": {"base"},
    "objectCreationExpression": {"type", "arguments"},
    "ternaryOperation": {"condition", "thenExpression", "elseExpression"},
    "instanceofExpression": {"lhs", "rhs"},  # 注意：这里rhs是类型，不是属性
    "annotation": {"annoValue", "annoMembers"},
    "literal": {"value"},
    "lambdaExpression": {"parameters", "body"},
    # 更多映射可以根据需要添加
}

# 特殊规则：某些属性在某些节点类型上不支持
# 例如：binaryOperation.operator 不支持（被映射为SkipRole）
# 从DSLRoleMapping.java中可以看到：
# - MoInfixExpression.operatorDescription -> SkipRole
# - MoPostfixExpression.operatorDescription -> SkipRole  
# - MoPrefixExpression.operatorDescription -> SkipRole
# 这意味着不能直接访问operator属性，应该使用节点类型检查
UNSUPPORTED_PROPERTY_PATHS = {
    ("binaryOperation", "operator"),  # MoInfixExpression.operator -> SkipRole
    ("unaryOperation", "operator"),    # MoPostfixExpression.operator 或 MoPrefixExpression.operator -> SkipRole
    ("postfixOperation", "operator"),   # MoPostfixExpression.operator -> SkipRole
    ("prefixOperation", "operator"),    # MoPrefixExpression.operator -> SkipRole
    # 更多不支持的路径可以根据需要添加
}

# 属性值类型映射：某些属性只能接受特定的值类型
# 例如：operator属性在某些情况下应该使用节点类型检查而不是属性检查
# 注意：由于operator属性被映射为SkipRole，不能直接访问
# 当LLM生成类似 binaryoperation_1.operator is instanceof 的条件时，
# 应该转换为 binaryoperation_1 is instanceofExpression
PROPERTY_VALUE_TYPE_HINTS = {
    # 当检查操作符类型时，应该使用节点类型检查
    # 例如：binaryoperation_1.operator is instanceof -> binaryoperation_1 is instanceofExpression
    "operator": {
        "instanceof": "instanceofExpression",
        # 注意：其他操作符（如 +, -, *, / 等）不能直接转换为节点类型
        # 这些操作符是InfixOperator节点，但binaryOperation本身已经表示二元操作
        # 如果需要检查操作符，应该检查子节点的类型
    }
}

# 操作符到节点类型的反向映射（用于修复建议）
# 当遇到 binaryoperation_1.operator is "instanceof" 时，应该转换为
# binaryoperation_1 is instanceofExpression
# 注意：这个映射用于将操作符值转换为对应的节点类型检查
OPERATOR_TO_NODE_TYPE = {
    "instanceof": "instanceofExpression",
    # 可以添加更多映射
    # 注意：大多数操作符（如 +, -, *, / 等）不能直接映射到节点类型
    # 这些操作符是InfixOperator节点，但binaryOperation本身已经表示二元操作
}
