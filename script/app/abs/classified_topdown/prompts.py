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
Note: According to the following template, please answer the question with 'yes' or 'no' at beginning:
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
Note: According to the following template, please answer the question with 'yes' or 'no' at beginning:
[yes/no]: [Cause analysis]
"""

REGEX_NAME_PROMPT = """Does this name have to be literally equal to `{value}`? Please evaluate whether it must literally \
equal to `{value}`, or it can be replace by another name with similarly semantic.
'yes': If the name must literally equal to `{value}`
'no': If the name can be replace by another name.

Note: 
1. Normally, common function call names must be literally equal. \
Customized function or variable names can be replaced by semantic like names.
2. If it can be replaced, please summarize the possible regular expressions based on your knowledge

Strictly follow the format below:
1. First part: "yes" or "no"
2. Second part (if yes): regex enclosed in double quotes ""
3. Separate parts with triple vertical bars (|||)
4. No explanations or formatting characters outside quotes

Examples:
Example1:
Name: setex
Output: yes|||"(setex|save|insert|update|put)"

Example2:
Name: excelFilePath
Output: yes|||"(?i).*(path)$"

Example3:
Name: getenv
Output: no|||""
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