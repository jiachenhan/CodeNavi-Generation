grammar DSL;

// DSL语法定义
// 基于BNF语法规则：
// Query ::= EntityDecl where Condition ;
// EntityDecl ::= NodeType (Alias)?
// Condition ::= AtomicCond | not (Condition) | and (Condition (, Condition)+) | or (Condition (, Condition)+)
// AtomicCond ::= ValueMatch | RelMatch
// ValueMatch ::= Attribute ValueComp Value
// RelMatch ::= Attribute NodeComp Query
// Attribute ::= Alias (.Property)*
// Value ::= NodeType | <var name> | literal value
// ValueComp ::= match | is | == | !=
// NodeComp ::= contain | in

// 词法规则
// 主查询：EntityDecl where Condition ;
// 注意：只有整个DSL的最末尾才有分号
query: entityDecl WHERE condition SEMICOLON EOF;

// 嵌套查询（用于contain/in中）：EntityDecl [where Condition]
// 注意：嵌套查询的where子句是可选的
// 在and()/or()中，嵌套查询后面直接跟逗号或右括号，没有分号
// 只有在作为最外层查询时，才需要分号（由query规则处理）
nestedQuery: entityDecl (WHERE condition)?;

entityDecl: nodeType alias?;

nodeType: IDENTIFIER;

alias: IDENTIFIER;

condition: atomicCondition
         | NOT LPAREN condition RPAREN
         | AND LPAREN condition (COMMA condition)+ RPAREN
         | OR LPAREN condition (COMMA condition)+ RPAREN;

atomicCondition: valueMatch | relMatch | attributeOnly;

// 值匹配：Attribute ValueComp Value
valueMatch: attribute valueComp value;

// 仅属性（布尔属性，隐式检查为true）：Attribute
// 例如：functiondeclaration_1.isSynchronized
attributeOnly: attribute;

// 关系匹配：Attribute NodeComp Query
// 注意：这里的Query是嵌套查询，不包含分号（分号只在最外层查询的末尾）
relMatch: attribute nodeComp nestedQuery;

attribute: alias (DOT propertyName)*;

propertyName: IDENTIFIER;

value: nodeType
     | identifier
     | stringLiteral
     | numberLiteral
     | booleanLiteral;

identifier: IDENTIFIER;

stringLiteral: STRING_LITERAL;

numberLiteral: NUMBER;

booleanLiteral: BOOLEAN;

valueComp: MATCH | IS | EQ | NE;

nodeComp: CONTAIN | IN | NOT_IN;

// 词法定义
// 注意：词法规则的顺序很重要，更具体的规则应该放在前面
// 字符串字面量应该优先匹配，避免字符串内的字符被其他规则匹配

// 字符串字面量：支持单引号和双引号，支持转义
// 注意：需要正确处理所有转义字符，包括 \s, \d, \., \|, \(, \) 等正则表达式中的转义
// 支持两种转义引号的方式：
//   1. 标准方式：\" (反斜杠转义)
//   2. SQL风格："" (双引号转义，用于兼容某些生成的DSL文件)
STRING_LITERAL: '"' (ESC_SEQ | '""' | ~["\\])* '"'
              | '\'' (ESC_SEQ | ~['\\])* '\'';

fragment ESC_SEQ: '\\' (ESC_CHAR | UNICODE | .);
fragment ESC_CHAR: ["'\\/bfnrt];
fragment UNICODE: 'u' HEX HEX HEX HEX;
fragment HEX: [0-9a-fA-F];

// 数字字面量：整数和浮点数
NUMBER: '-'? INT ('.' [0-9]+)? EXP?;
fragment INT: '0' | [1-9] [0-9]*;
fragment EXP: [eE] [+\-]? [0-9]+;

// 布尔字面量
BOOLEAN: 'true' | 'false';

// 关键字（必须在标识符之前）
WHERE: 'where';
NOT: 'not';
AND: 'and';
OR: 'or';
MATCH: 'match';
IS: 'is';
CONTAIN: 'contain';
IN: 'in';
NOT_IN: 'notIn';

// 操作符
EQ: '==';
NE: '!=';

// 标点符号
LPAREN: '(';
RPAREN: ')';
DOT: '.';
COMMA: ',';
SEMICOLON: ';';

// 标识符：字母、数字、下划线，必须以字母或下划线开头（必须在关键字之后）
IDENTIFIER: [a-zA-Z_][a-zA-Z0-9_]*;

// 空白字符：空格、制表符、换行符等
WS: [ \t\r\n]+ -> skip;

// 注释（可选，用于未来扩展）
LINE_COMMENT: '//' ~[\r\n]* -> skip;
BLOCK_COMMENT: '/*' .*? '*/' -> skip;
