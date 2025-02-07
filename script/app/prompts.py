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
{original_code}
```

Your task is to:
Step 1 - Analyzing the semantic of this modification: Understand the purpose and significance of the changes within \
the context of code.

"""

TASK_DESCRIPTION_PROMPT = """
"""

NORMAL_ELEMENT_PROMPT = """
"""

NAME_ELEMENT_PROMPT = """
"""

EXPRESSION_TYPE_ELEMENT_PROMPT = """
"""

STRUCTURE_ELEMENT_PROMPT = """
"""

