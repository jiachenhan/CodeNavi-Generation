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
    functionCall f1 where f1.name match "(?i).*set(Access|Visibility|Field)";
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

Note: A code line is critical if:
1. This line contains contextual features related to the violation.
2. This line contains code elements that may appear in similar patterns.

Note: If this violation occurs more than once, only keep one and record their line numbers.
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

NORMAL_ELEMENT_PROMPT = """Based on your analysis, \
for the code element AST node{{ type:{elementType} value:{element} in line {line} }}, which is a
child node of {parentElement}, please classify its violation relevance \
by selecting ALL applicable types from following categories:

Violation information: {error_info}

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

NAME_ELEMENT_PROMPT = """Please evaluate whether the name of the element `{element}` in line {line} is representative \
for above violation(s).
'Yes': If the name is important Node for above violation(s).
'No': If the name is not important Node for above violation(s).

Note: You can refer to your analysis above to determine which nodes are important.
Note: According to the following template, please answer the question with 'yes' or 'no' at beginning:
[yes/no]: [Cause analysis]
"""

LITERAL_ELEMENT_PROMPT = """Please evaluate whether the string literal `{element}` in line {line} is representative \
for above violation(s).
'Yes': If the StringLiteral is important Node for above violation(s).
'No': If the StringLiteral is not important Node for above violation(s).

Note: You can refer to your analysis above to determine which nodes are important.
Note: According to the following template, please answer the question with 'yes' or 'no' at beginning:
[yes/no]: [Cause analysis]
"""

REGEX_NAME_PROMPT = """Does this name have to be literally equal to `{value}`? Please evaluate whether it must literally \
equal to `{value}`, or it can be replace by another name with similarly semantic.
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
Please evaluate whether it must literally equal to `{value}`, or it can be represented by a regular expression

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


AFTER_TREE_TASK_PROMPT = """Based on your analysis, the fixed version code fixes some issues by adding some code snippet.\
I want to detect similar problem codes that still have issues as they have not been fixed with similar modifications.

I will provide you with the elements of the Abstract Syntax Tree (AST) from the fixed code.
Your task is to analyze each AST element and determine whether it is representative.

Note: A code element is considered representative if:
* Its semantic information is useful in correcting incorrect code

Note: You should NOT attempt to split AST by yourself.
"""

AFTER_TREE_ELEMENT_PROMPT = """For AST type `{elementType}` code element `{element}` in fixed AST, please \
analyze whether it contains representative code elements for resolving above violations(s).
'Yes': If the code snippet contains representative code element for resolving above violation(s).
'No': If the code snippet does not contain representative code element for resolving above violation(s).

Note: A code element is considered representative if it meets any of the following criteria:
1. Direct Contribution: It directly contributes to resolving the violation(s).
2. Include key semantics: It contains key code, whose semantics are helping to solve this violation(s).
Note: According to the following template, please answer the question with 'yes' or 'no' at beginning:
[yes/no]: [Cause analysis]
"""

AFTER_TREE_NAME_PROMPT = """Please evaluate whether the name of the element `{element}` is representative \
for resolving above violation(s).
'Yes': If the name is representative for resolving above violation(s).
'No': If the name is not representative for resolving above violation(s).

Note: A code element's name is considered representative if it meets any of the following criteria:
1. Refers to a function, a class or interface: this function call, class or interface is crucial for solving this problem.
2. Key semantic: the name's semantic is crucial for solving this problem.
Note: According to the following template, please answer the question with 'yes' or 'no' at beginning:
[yes/no]: [Cause analysis]
"""