TASK_DESCRIPTION_PROMPT = """Based on your analysis, I've identified that there are some representative code elements \
that clearly illustrate the violation(s) and strongly related to the causes. These representative code elements can \
be used to detect similar violation(s) in other parts of the codebase.

I will provide you with the elements of the Abstract Syntax Tree (AST) from the original code.
Your task is to analyze each AST element and select which parts of the element are representative.

Note: A code element is considered representative if:
* It directly contributes to triggering the violations.
* Its semantics, type, structure are strongly indicative of the underlying violations.
* It is commonly observed in similar violation patterns based on your knowledge.

Note: You should NOT attempt to split AST by yourself.
"""

SELECT_STMT_PROMPT = """Here are some marked statements at the following, your task is select the statements which \
contain representative code elements for above violation(s).

Statements Here:
{stmt_list_prompt}

Note: A code element is considered representative if it meets any of the following criteria:
1. Direct Contribution: It directly contributes to triggering the violation(s).
2. Include violated code: It contains the code triggering the violation(s).
3. Key features: It contains relevant features that may appear in the context of this violation(s).
4. Common Pattern: It is commonly observed in similar violation patterns based on your knowledge.

Note: Please select statements with the following template, beginning with a Python list containing \
key statement numbers. If there are no statements contain representative code elements, please answer with a empty \
Python list.

---
Answer:
[1, 2, 3, 5, 8]
Cause:
Your analysis here.
---
"""

SELECT_ELEMENT_PROMPT = """Which parts contain representative code elements in the following for above violation(s)?

Code Elements Here:
{element_list_prompt}

Note: A code element is considered representative if it meets any of the following criteria:
1. Direct Contribution: It directly contributes to triggering the violation(s).
2. Include violated code: It contains the code triggering the violation(s).
3. Key features: It contains relevant features that may appear in the context of this violation(s).
4. Common Pattern: It is commonly observed in similar violation patterns based on your knowledge.

Note: Please select elements with the following template, beginning with a Python list containing \
key part numbers. If there are no parts contain representative code elements, please answer with a empty \
Python list.

---
Answer:
[1, 2, 3, 5, 8]
Cause:
Your analysis here.
---

"""

AFTER_TREE_TASK_PROMPT = """Based on your analysis, the fixed version code fixes some issues by adding some code snippet.\
I want to detect similar problem codes that still have issues as they have not been fixed with similar modifications.

I will provide you with the elements of the Abstract Syntax Tree (AST) from the fixed code.
Your task is to analyze each AST element and select which parts of the element are representative.

Note: A code element is considered representative if:
* Its semantic information is useful in correcting incorrect code

Note: You should NOT attempt to split AST by yourself.
"""

AFTER_TREE_ELEMENT_PROMPT = """Which parts contain representative code elements in the following for \
resolving above violation(s)?

Code Elements Here:
{element_list_prompt}

Note: A code element is considered representative if it meets any of the following criteria:
1. Direct Contribution: It directly contributes to resolving the violation(s).
2. Include key semantics: It contains key code, whose semantics are helping to solve this violation(s).

Note: Please select elements with the following template, beginning with a Python list containing \
key part numbers. If there are no parts contain representative code elements, please answer with a empty \
Python list.

---
Answer:
[1, 2, 3, 5, 8]
Cause:
Your analysis here.
---

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
