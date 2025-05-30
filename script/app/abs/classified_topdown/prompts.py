TASK_DESCRIPTION_PROMPT = """I hope to summarize the violations in the code into defect templates, and the template \
can be used to detect similar violation in other codebase. I will provide you with the elements of \
the Abstract Syntax Tree (AST) from the original code. And your task is to analyze each AST Node and determine \
whether it should appear in template.

For example: \
The Error code is
```java
    Class c = new String().getClass();
```

The violation info is
```
Avoid instantiating an object just to call getClass() on it; use the .class public member instead.
```

Your should think like the following:
* Step 1: Analysis the violation
The violation means that new String() is an object creation, and method invocation "getClass()" shouldn't be called \
on an objectCreation Node.
* Step 2: Guessing possible defect templates
This template can be described as a DSL like the following:
```DSL
    functionCall f1 where and(
        f1.base is ObjectCreation,
        f1.name == "getClass"
    );
```
This DSL can retrieve all method invocation which name is 'getClass' and its target is an object creation. \
And you can summarize other defects to DSL, which can effectively describe such defects.

For Name and Literal nodes, if the literal value can be summarized by regular expressions, this node is also important.
For example the following DSL.
```DSL
    functionCall f1 where f1.name match "(?i).*set(Accessible|Visiable)";
```

* Step 3: Mapping DSL to the important AST Nodes
Based on the DSL, the important AST Nodes are
1. MethodInvocation `new String().getClass()`
2. ClassCreation `new String()`
3. SimpleName `getClass`

After this analysis process, you can know which AST nodes are important.
"""

ROUGH_SELECT_LINES_PROMPT = """Based on your analysis, Please select which lines are critical\
for this violation and record their line numbers. 

Note: A code line is critical if it is part of a defective code snippet.
Note: If this kind of violation occurs more than once, you only need to keep the line numbers \
of one defective code fragment and ignore the other defective code lines. \
Because the template will be accurate if only use one code snippet.

For example:
The buggy code
```java
    2: public void testNewReader() throws IOException {
    3: File asciiFile = getTestFile("ascii.txt");
    4: try {
    5:   Files.newReader(asciiFile, null);
    6:   fail("expected exception");
    7: } catch (NullPointerException expected) {
    8: }
    9: 
    10: try {
    11:   Files.newReader(null, Charsets.UTF_8);
    12:   fail("expected exception");
    13: } catch (NullPointerException expected) {
    14: }
    16: 
    17: BufferedReader r = Files.newReader(asciiFile, Charsets.US_ASCII);
    18: try {
    19:   assertEquals(ASCII, r.readLine());
    20: } finally {
    21:   r.close();
    22: }
  }
```
the violation information is \
'AvoidCatchingNPE: Avoid catching NullPointerException; consider removing the cause of the NPE.'

Because line 4-8 and line 10-14 both represent the same issue, so import lines only reserve one of them.
critical lines : [4, 5, 6, 7, 8]


Strictly follow the format below:
1. First part: [critical lines]
2. Second part: A number list wrapped in a pair of square brackets.
3. Third part: your analysis
Each part use ||| segmentation between three parts

Example output:
[critical lines] ||| [4, 5, 6, 7, 8] ||| \n your analysis
"""

NORMAL_TOP_ELEMENT_PROMPT = """Based on your analysis, \
for the code element AST node{{ type:{elementType} value:{element} in line {line} }}, \
please classify its violation relevance by selecting ALL applicable types from following categories:

Violation information: {error_info}

[Category Options]
 1. Important AST Node: This code element itself should appear in DSL Template.
 2. Structural Irrelevant: This code element shouldn't appear in DSL Template, but it contains important AST Node.
 3. Completely Irrelevant: This code element is classified as irrelevant if it does not meet any of the above criteria.

[Response Requirements]
Select one most relevant type number (1-3) for this element, and analyze the reason for your selection.
If no type is applicable, select 0.

Your response should be formatted as follows:
[Response Format]
[Type number]: [Corresponding analysis]

Example output:
[1]: [your analysis]
"""

NORMAL_ELEMENT_PROMPT = """Based on your analysis, \
for the code element AST node{{ type:{elementType} value:{element} in line {line} }}, which is a
child node of {parentElement}, please classify its violation relevance \
by selecting ALL applicable types from following categories:

Violation information: {error_info}

[Category Options]
 1. Important AST Node: This code element itself should appear in DSL Template.
 2. Structural Irrelevant: This code element shouldn't appear in DSL Template, but it contains important AST Node.
 3. Completely Irrelevant: This code element is classified as irrelevant if it does not meet any of the above criteria.
 
[Response Requirements]
Select one most relevant type number (1-3) for this element, and analyze the reason for your selection.
If no type is applicable, select 0.

Your response should be formatted as follows:
[Response Format]
[Type number]: [Corresponding analysis]

Example output:
[1]: [your analysis]
"""

NAME_ELEMENT_PROMPT = """Please evaluate whether the name of the element `{element}` in line {line} is representative \
for above violation(s).
'Yes': This name or the regular expression that summarizes it should appear in DSL.
'No': This name or the regular expression that summarizes it should not appear in DSL.

Note: Carefully check the position of this name regarding the code.
Note: According to the following template, please answer the question with 'yes' or 'no' at beginning:
[yes/no]: [Cause analysis]

Example output:
yes: The element name is representative for the violation, ...
"""

LITERAL_ELEMENT_PROMPT = """Please evaluate whether the string literal `{element}` in line {line} is representative \
for above violation(s).
'Yes': This StringLiteral or the regular expression that summarizes it should appear in DSL.
'No': This StringLiteral or the regular expression that summarizes it should not appear in DSL.

Note: According to the following template, please answer the question with 'yes' or 'no' at beginning:
[yes/no]: [Cause analysis]

Example output:
no: The string literal is not directly relevant to the violation...
"""

REGEX_NAME_PROMPT = """Does this name have to be literally equal to `{value}`? Please evaluate whether it must literally \
equal to `{value}`, or it can be better summarized by a regular expression to express this defect template.
'yes': If the name can be replace by another name. 
'no': If the name must literally equal to `{value}`

Note: 
1. Normally, common function call names must be literally equal. \
Customized function or variable names can be replaced by semantic like names.
2. If it can be replaced, please summarize the possible regular expressions based on your knowledge\
(Ensure the original name can be matched with regular expressions)

Strictly follow the format below:
1. First part: "yes" or "no"
2. Second part (if yes): possible regular expression; (if no): None
3. Third part: your analysis
Each part use ||| segmentation between three parts

Examples:
Example1:
Name: setex
Output: yes|||(setex|save|insert|update|put)||| \n your analysis

Example2:
Name: excelFilePath
Output: yes|||(?i).*(path)$||| \n your analysis

Example3:
Name: getenv
Output: no|||None||| \n your analysis
"""

REGEX_LITERAL_PROMPT = """Does this string literal have to be literally equal to `{value}`? \
Please evaluate whether it must literally equal to `{value}`, \
or it can be better summarized by a regular expression to express this defect template.

'yes': If the string literal can be represented by a regular expression
'no': If the string literal must literally equal to `{value}`

Note: 
1. Normally, most string literals have key parts related to violation, the given regular expression \
should generalize the key parts and arbitrary match the remaining parts
2. If it can be represented, please summarize the possible regular expressions based on your knowledge\
(Ensure the original string literals can be matched with regular expressions)

Strictly follow the format below:
1. First part: "yes" or "no"
2. Second part (if yes): possible regular expression; (if no): None
3. Third part: your analysis
Each part use ||| segmentation between three parts

Examples:
Example1:
String literal: pkgArrayList
Output: yes|||(?i)(pkg|package).*(list)||| \n your analysis

Example2:
String literal: System error
Output: yes|||(?i).*(error|warn)||| \n your analysis

Example3:
String literal: /**
Output: no|||None||| \n your analysis
"""

EXPRESSION_TYPE_ELEMENT_PROMPT = """
"""

STRUCTURE_ELEMENT_PROMPT = """Is the `{elementType}` structural framework of this code snippet `{element}` representative \
for the violation(s)? Or is it simply because it contains representative code elements?
'Yes': If the structural framework is representative for above violation(s).
'No': If the structural framework is not representative for above violation(s).

Note: A structural framework is considered representative if it meets any of the following criteria:
1. Control flow: The control flow structure is a necessary operation in violation.
2. Common Pattern: It is commonly observed in similar violation patterns based on your knowledge.
Note: According to the following template, please answer the question with 'yes' or 'no' at beginning:
[yes/no]: [Cause analysis]
"""


AFTER_TREE_TASK_PROMPT = """In addition, some code element in after tree also should be add into the defect template.
I will provide you with the elements of the Abstract Syntax Tree (AST) from the fixed code. 
And your task is to analyze each AST Node and determine whether it should appear in template.

For example: \
The code diff is
```java
    + if (LOG.isDebugEnabled()) {
        LOG.debug("get version from meta service, partitions: {}, versions: {}", partitionIds, versions);
    + }
```

The violation info is
```
GuardLogStatement: Logger calls should be surrounded by log level guards.
```

Fixed code should also appear in DSL template in Step 2:
* Step 2: Guessing possible defect templates
This template can be described as a DSL like the following:
```DSL
    functionCall f1 where and(
        f1.base.name match "(?i)(log|logger)",
        f1.name == "debug",
        f1 notin ifBlock ib where and(
            ib.condition.name == "isDebugEnabled"
        )
    );
```

* Step 3: Mapping DSL to the important AST Nodes
Based on the DSL, the important AST Nodes in before tree are
1. MethodInvocation `LOG.debug("get version from meta service, partitions: {}, versions: {}", partitionIds, versions)`
2. SimpleName `debug`
3. SimpleName `LOG`
import AST Nodes in after tree:
4. ifStatement `if (LOG.isDebugEnabled()) { LOG.debug("get version from meta service, partitions: {}, versions: {}", partitionIds, versions); }`
5. SimpleName `isDebugEnabled`

After this analysis process, you can know which AST nodes are important.
"""


AFTER_SELECT_LINES_PROMPT = """Please select which lines are critical in fixed code \
for this violation and record their line numbers. 

Note: A code line is critical if:
1. This line contains contextual features related to the violation.
2. This line contains code elements that may appear in similar patterns.

Note: If this violation occurs more than once, only keep one and record their line numbers.
For example:
The buggy code
```java
    19: if (LOG.isDebugEnabled()) {
    20:     LOG.debug("getVisibleVersion use CloudPartition {}", partitionIds.toString());
    21: }
    22: Cloud.GetVersionResponse resp = getVersionFromMeta(req);
    23: if (resp.getStatus().getCode() != MetaServiceCode.OK) {
    24:     throw new RpcException("get visible version", "unexpected status " + resp.getStatus());
    25: }
    26: 
    27: List<Long> versions = resp.getVersionsList();
    28: if (versions.size() != partitionIds.size()) {
    29:     throw new RpcException("get visible version",
    30:             "wrong number of versions, required " + partitionIds.size() + ", but got " + versions.size());
    31: }
    32:
    33: if (LOG.isDebugEnabled()) {
    34:     LOG.debug("get version from meta service, partitions: {}, versions: {}", partitionIds, versions);
    35: }
  }
```
the violation information is \
'GuardLogStatement: Logger calls should be surrounded by log level guards.'

Because line 19-21 and line 33-35 both represent the same issue, so import lines only reserve one of them.
critical lines : [19, 20, 21]


Strictly follow the format below:
1. First part: [critical lines]
2. Second part: A number list wrapped in a pair of square brackets.
3. Third part: your analysis
Each part use ||| segmentation between three parts

Example output:
[critical lines] ||| [19, 20, 21] ||| \n your analysis
"""

CORE_EDIT_PROMPT = """Based on your analysis, \
please classify the edit operation {{edit}} in the following categories:

[Category Options]
 1. Core operation: the edit operation has a core contribution to introducing semantic fixes for defects
 2. Auxiliary operation: the editing operation is designed to accommodate the addition of core operations, \
 (including modifying variable scopes or refactoring local code)
 
[Response Requirements]
Select one most relevant type number (1-2) for this element, and analyze the reason for your selection.

Your response should be formatted as follows:
[Response Format]
[Type number]: [Corresponding analysis]

Example output:
[1]: [your analysis]
"""


AFTER_TREE_ELEMENT_PROMPT = """Based on your analysis, \
for the code element AST node{{ type:{elementType} value:{element} in line {line} }}, \
please classify its violation relevance by selecting ALL applicable types from following categories:

[Category Options]
 1. Important AST Node: One code element itself should appear in DSL Template.
 2. Structural Irrelevant: One code element shouldn't appear in DSL Template, but it contains important AST Node.
 3. Completely Irrelevant: One code element is classified as irrelevant if it does not meet any of the above criteria.
 
[Response Requirements]
Select one most relevant type number (1-3) for this element, and analyze the reason for your selection.
If no type is applicable, select 0.

Your response should be formatted as follows:
[Response Format]
[Type number]: [Corresponding analysis]

Example output:
[1]: [your analysis]
"""

AFTER_TREE_NAME_PROMPT = """Please evaluate whether the name of the element `{element}` \
in line {line} is representative for above violation(s).
'Yes': If the name is important Node for above violation(s).
'No': If the name is not important Node for above violation(s).

Note: You can refer to your analysis above to determine which nodes are important.
Note: According to the following template, please answer the question with 'yes' or 'no' at beginning:
[yes/no]: [Cause analysis]
"""