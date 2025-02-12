BACKGROUND_PROMPT = """You are an expert in identifying and resolving security vulnerabilities and code violations, \
with a deep understanding of java code analysis.
I will provide you a piece of code and its modification, the modification is a fixing of violations in the original \
code.

Here is the original code:
```java
{original_code}
```

Here is the modification information:
```
{change_info}
```

Your task is to:
Step 1 - Analyzing the semantic of this modification: Understand the purpose and significance of the changes within \
the context of code.
Step 2 - Identify the violations addressed: Determine the specific problem or deficiency the modification resolves, such \
as a bug, performance issue, or security vulnerability.
please think step by step and if you are unsure of the meaning of this modification, \
please tell me directly 'I don't know the purse of this modification'. 
"""

TASK_DESCRIPTION_PROMPT = """Based on your analysis, I've identified that there are some representative code elements \
that clearly illustrate the violation(s) and strongly related to the causes. These representative code elements can \
be used to detect similar violation(s) in other parts of the codebase.

I will provide you with the elements of the Abstract Syntax Tree (AST) from the original code.
Your task is to analyze each AST element and determine whether it is representative.

Note: A code element is considered representative if:
* It directly contributes to triggering the violations.
* Its semantics, type, structure are strongly indicative of the underlying violations.
* It is commonly observed in similar violation patterns based on your knowledge.

Note: You should NOT attempt to split AST by yourself.
"""

NORMAL_ELEMENT_PROMPT = """For AST type `{elementType}` code element `{element}` in line `{line}`, please \
analyze whether it contains representative code elements for above violations(s).
'Yes': If the code snippet contains representative code element for above violation(s).
'No': If the code snippet does not contain representative code element for above violation(s).

Note: A code element is considered representative if it meets any of the following criteria:
1. Direct Contribution: It directly contributes to triggering the violation(s).
2. Include violated code: It contains the code triggering the violation(s).
3. Key features: It contains relevant features that may appear in the context of this violation(s).
4. Common Pattern: It is commonly observed in similar violation patterns based on your knowledge.
Please answer the question according to the following template:
[yes/no]: [Cause analysis]
"""

NAME_ELEMENT_PROMPT = """Please evaluate whether the name of the element `{element}` in line {line} is representative \
for above violation(s).
'Yes': If the name is representative for above violation(s).
'No': If the name is not representative for above violation(s).

Note: A code element's name is considered representative if it meets any of the following criteria:
1. Semantic meaning of variables: The semantic meaning contained in the name suggests that it may trigger the violation(s), \
or its semantic meaning is related to the modification.
2. Refers to a violated function: Represents violated function calls, type constructions, etc.
3. Key features: The name is the feature related to the modifications that need to be applied
4. Common Pattern: It is commonly observed in similar violation patterns based on your knowledge.
Please answer the question according to the following template:
[yes/no]: [Cause analysis]
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
Please answer the question according to the following template:
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

AFTER_TREE_ELEMENT_PROMPT = """For code element `{element}`, which is `{elementType}` type in fixed AST, please \
analyze whether it contains representative code elements for resolving above violations(s).
'Yes': If the code snippet contains representative code element for resolving above violation(s).
'No': If the code snippet does not contain representative code element for resolving above violation(s).

Note: A code element is considered representative if it meets any of the following criteria:
1. Direct Contribution: It directly contributes to resolving the violation(s).
2. Include key semantics: It contains key code, whose semantics are the core of solving this violation(s).
Please answer the question according to the following template:
[yes/no]: [Cause analysis]
"""

AFTER_TREE_NAME_PROMPT = """Please evaluate whether the name of the element `{element}` is representative \
for resolving above violation(s).
'Yes': If the name is representative for resolving above violation(s).
'No': If the name is not representative for resolving above violation(s).

Note: A code element's name is considered representative if it meets any of the following criteria:
1. Refers to a function: this function call is crucial for solving this problem.
[yes/no]: [Cause analysis]
"""