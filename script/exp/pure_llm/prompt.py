from pathlib import Path

PURE_LLM_PROMPT_V1 = """
### CodeNavi DSL Rule:
1. Rule Introduction
1.1 Node Query Rules
Node query rules can filter out specific program elements, which we refer to as nodes in CodeNavi. \
For example: method definition node, method call expression node, class definition node.

1.1.1 Simple Example
Here is an example of a node query rule:

```CodeNavi
functionDeclaration fd1 where
    and(fd1.hasBody,
        fd1.name startWith "debug",
        fd1.parameters.size() == 1,
        fd1.parameters[0].type.name == "java.util.List");
```
The above rules will filter out all names in user code that start with debug and only contain one Methods for List type parameters.

* The query condition of this rule is a composite condition consisting of four sub conditions linked by the logical conjunction 'and'. \
Each sub condition is applied to a node or its attribute through a specific operator.
* The hasBody in the rules, Name and parameters are the attributes of the node. \
HasBody is a Boolean property, name is a string property, and parameters is a collection property. \
We will provide a detailed introduction to the types of attributes in the chapter on attributes.
* Fd1 is an alias for the functionDeclaration node. When writing DSL rules, \
defining aliases can significantly enhance the readability of the rules. \
It is recommended to define an alias for each node.
* The `startWith`, `==` in the rule are operators that filter nodes. \
The boolean attribute hasBody implicitly contains an operator, \
which we will provide a detailed introduction to in the boolean attribute section
* Some attributes can have their own attributes, which we refer to as node attributes in DSL. \
They belong to a certain node and are also a node themselves. \
We can pass through Retrieve the attributes of node properties using keywords. \
`Parameters[0].type.name` first retrieves the attribute type of node property parameters[0]. \
The latter is also a node attribute, and we obtain the attribute name of the type.

1.1.2 Querying Node Attributes
If the attributes of a node are also nodes, we can also write rules to filter the attributes of nodes.

```CodeNavi
functionCall.arguments fa where
    and(fa.argumentIndex == 1,
        fa.type.name == "java.lang.String");
```
In the above rules, we filtered out the second parameter of the method call type as String.

1.1.3 Nested Query
The attribute of a node can also be a node, and for node attributes, we can write nested query rules.

```CodeNavi
functionCall fc1 where
    and(fc1.name == "hello",
        fc1.enclosingFunction ef where
            ef contain functionCall fc2 where
                and(fc2.arguments[0].variable is fc1.arguments[0].variable,
                    fc2 isnot fc1));
```
In the above rules, we have separately written sub query rules for ef and fc2.

1.1.4 Recursive Query
We can write recursive query rules using the asterisk *.

```CodeNavi
functionDeclaration fd where
    fd.(functionCalls.function)* targetFd where
        targetFd is fd;
```
In the above rules, functionCalls.function will be repeatedly called until targetFd meets specific query conditions.
If there is only one attribute of the node to be recursive, parentheses can be omitted. For example:
```CodeNavi
functionCall fc where fc.arguments* == null;
```

1.1.5 String Operations
We have built-in utility functions for more complex query operations on string properties.

```CodeNavi
functionDeclaration fd where
    or(fd.name + "world" == "helloworld",
       "hello" + fd.name == "helloworld",
       "hell" + fd.name + "orld" == "helloworld",
       fd.name.capitalize() == "hello" + fd.enclosingClass.name.capitalize(),
       fd.name.toUpperCase().toLowerCase() == "lower");
```

2. Logical connectors
Logical connectives are keywords that act on query conditions. DSL supports `and`, `or`, `not` three related words.

Logical connectors need to be used together with parentheses, where the specific query conditions are indicated. \
Among them, 'and' and 'or' can be applied to multiple query conditions \
(i.e. parentheses can contain multiple query conditions separated by commas), \
while 'not' can only be applied to one query condition. \
Logical connectives are a type of special operator that acts on query conditions \
(ordinary operators act on nodes or attributes), and their specific meanings are as follows:

* and: The query conditions in parentheses must all be true
* or: At least one of the query conditions in parentheses is true
* not: The query condition in parentheses is false

It should be noted that the query conditions for logical connectors can also include logical connectors. \
For example, the following rule filters methods called debug or foo with 1 or 3 parameters.

```CodeNavi
functionDeclaration fd where
    and(or(fd.name == "debug", fd.name == "foo"),
        or(fd.parameters.size() == 1, fd.parameters.size() == 3));
```

3. Operators
Query conditions are applied to specific nodes or attributes through operators to complete node filtering. \
The operators supported by DSL include:
>(<=), <(>=), !, [], ==(!=), startWith(notStartWith), endWith(notEndWith), contain(notContain), match(notMatch), is(isnot), in(notIn)
The operator in parentheses is the inverse operator of the corresponding operator, semantically equivalent to the not connector.

3.1 General Operators (==, contain)
==(!=) operators can be applied to numeric properties, string properties, Boolean properties, object properties, and node properties. \
No matter what property it applies to, the left side of the operator must be a property, and the right side must be a constant.

* When applied to numerical properties, the right side is an integer or decimal. For example: `arguments.size() == 3`
* When applied to string properties, the string constant is on the right side. For example: `name == “foo”`
* When applied to Boolean properties, the right-hand side is the Boolean constant true or false. For example: `isPublic == true`
* When applied to object properties, the right side represents integers, decimals, strings, or Boolean constants. For example: `value == 10`
* When applied to node properties, the right side is an empty constant null. For example: `initializer == null`

The contain(notContain) operator can be applied to string properties and node set properties.

* When applied to string properties, the right-hand side is a string constant, \
and returns true when the constant is a substring of the property. \
For example: `recordDeclaration where name contain “debug”;` \
Return the class declaration node with debug included in its name
* When applied to node set attributes, the query condition is on the right side, \
and true is returned when the set attributes contain nodes that meet this condition. \
For example: `functionDeclaration where parameters contain param where param.name == “i1”;` \
Return a method declaration containing a parameter named i1

3.2 Arithmetic Operators (>,<)
Arithmetic operators apply to numerical and object properties

3.3 String Operators (startWith, endWith, match)
The string operator applies to both string and object properties, with string constants to the right of the operator.

* `functionDeclartion where name startWith “debug”` Filter out method declarations with names starting with debug
* `FunctionDeclarion where name endWith "hello"` Filter out method declarations with names ending in "hello"
* `Function Declaration where name matches ". * (login). *"` Filter out name matching regular expressions* Method declaration for (login). *

3.4 Boolean operator (!)
The ! Operator is located to the left of the Boolean property, indicating that the Boolean property value is false. \
For example: `recordDeclaration where !isPublic` is equivalent to `recordDeclaration where isPublic==false`

3.5 Node operator (contain, in, is)
The left side of the node operator can be a node attribute or alias.

3.5.1 contain
'contain' is used to query child nodes on the syntax tree structure.
For example, the following rules filter out method declarations that call an nonparametric method named foo inside the method body.

```CodeNavi
functionDeclaration fd where
    fd contain functionCall where
        and(name == "foo",
            arguments.size() == 0));
```

3.5.2 in
The semantics of 'in' are opposite to 'contain'. Inquery the parent node on the syntax tree structure.
For example, the following rules filter out data member access nodes located in the assignment expression.

```CodeNavi
fieldAccess fa1 where
    fa1 in assignStatement;
```

3.5.3 is
To the right of is can be node attributes or aliases.
For example, the following rules filter out variable access nodes that appear on the left side of the assignment expression, \
with method call nodes on the right side of the assignment expression. \
The 'va' in the rule is an alias for the node 'variableAccess'.

```CodeNavi
variableAccess va where
    va in assignStatement where
        and(lhs is va,
            rhs is functionCall);
```

4. Attributes
The DSL engine selects nodes whose attributes meet specific conditions through operators. \
Attributes include six types: numbers, strings, Booleans, objects, nodes, and collections. \
Among them, set attributes can be divided into five subtypes: number set, string set, Boolean set, object set, and node set.

4.1 Numerical Attributes
The value of a numerical attribute can be an integer or a decimal.

4.2 String Attributes
The value of a string property can be a string constant.

4.3 Boolean Attributes
The value of a Boolean property can be a Boolean constant of true or false. Boolean attributes can be used independently. \
When used alone, Boolean properties are implicitly equivalent to a universal conditional expression containing==.

For example, `recordDeclaration where isPublic;`  Equivalent to `recordDeclaration where isPublic==true;`

4.4 Object Attributes
The values of object properties may be integers, decimals, strings, or Boolean constants.

4.5 Node Attributes
The node attribute itself is a node that can have its own attributes.

4.5.1 Alias
Nodes or attributes can have their own aliases, which can be used in conjunction with the is operator. \
The naming convention for aliases is as follows:
* Can only contain uppercase and lowercase English letters and numbers;
* Numbers can only appear at the end;
* Cannot have the same name as a built-in node or property in CodeNavi.

For example, va1, fd, and Field1 are all legal aliases, while 1fc and v2a are illegal aliases. \
In addition, the query criteria for node set attributes must include aliases.
After defining aliases, we can perform more complex query operations. \
In the following code, we have defined aliases for both functionCall and ifBlock, \
and compared their starting line numbers using the aliases.
Obviously, without defining aliases, we cannot complete similar queries.

```CodeNavi
functionCall fc where
    fc.enclosingFunction contain ifBlock ifb where
        ifb.startLine < fc.startLine;
```
Note: To improve code clarity, it is recommended to define aliases for all nodes and attributes that contain query conditions

4.6 Node Set Attributes
When node set attributes and contacts are used together, they must include aliases, as shown below:
```CodeNavi
functionDeclaration fd1 where
    fd1.parameters contain param where
        param.name startWith "register";
```

5. Keywords and constants
5.1 Index Keywords ([])
By indexing keywords, the corresponding subscript elements in the collection can be obtained. \
In the example rule, we obtained the first parameter of the method by indexing keywords. It should be noted that index indices start from 0.

5.2 Alias Keywords (as)
When using aliases, they can be explicitly defined using the 'as' keyword, for example: 
`functionCall as fc1 where fc1.name == “helloWorld”;`
Note: This keyword is optional and equivalent to `functionCall fc1 where fc1.name=="helloWorld";`

5.3 Size() keyword
The number of attributes can be obtained through size(), which is generally used for collecting attributes. \
In the example rule, we obtained the number of method parameters through size(). \
When used for non collection attributes, we can determine whether a certain attribute exists. For example:
`fieldDeclaration where initializer.size() == 0;` Filter out data member declarations that do not display initialization.

5.4 Empty Constant (null)
An empty constant refers to null in the source code. For example: `private String field = null、str = null` \
When used, empty constants often appear together with==(!=). For example:

```CodeNavi
fieldDeclaration fd where fd.initializer == null;
assignStatement as1 where as1.rhs == null;
```

5.5 Numerical constants
The numerical constants in DSL can be integers or decimals.

5.6 String Constants
String constants support uppercase and lowercase English letters, numbers, and special characters in regular expressions.

### Task:
I am using CodeNavi DSL rules to write some code checking rules, with the aim of \
checking the rules of the code written by developers. 
I will provide you with a set of positive-example and counter-example Java code and a defect description, \
the counter-example is a poor implementation that may lead to security issues or bugs.
The CodeNavi DSL describes incorrect implementation code to detect possible similar defects.

Your task is to construct DSL code that can detect this type of defect and identify counter-examples.

### Examples:

Example 1:
positive-example java code:
```java
logger.error(e.getMessage());
```

counter-example java code:
```java
logger.error(e);
```

CodeNavi DSL:
```
functionCall fc where
    and(
        fc.base.name match "log.*",
        fc.name == "error"
        fc.arguments[0] arg where
            or(
                arg is VariableAccess,
                arg.name == "e",
                not(
                    and(
                        arg is functionCall,
                        arg.name == "getMessage"
                    )
                )
            )
    );
```

The purpose of the rules:

This DSL will capture all log error outputs, and parameter 0 cannot be a call to the getMessage function.
Capturing such code indicates a violation and requiring developers to ensure the accuracy of log output.

Example 2:
positive-example java code:
```java
FileReader reader = new FileReader(in)
```

counter-example java code:
```java
FileReader reader = UsFileUtils.getFileReader(in)
```

CodeNavi DSL:
```
objectCreationExpression where
    name == "FileReader";
```

The purpose of the rules:

This DSL will capture all `FileReader` object creating.
Developers should use secure FileReader object creation methods to avoid cross directory attacks

### Input:
Defect Description: {description}

counter-example java code: {buggy}

positive-example java code: {fixed}


### Output:
Note: Your code should begin with '```CodeNavi' and end with another ```
CodeNavi DSL:
"""
