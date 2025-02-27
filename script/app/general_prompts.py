BACKGROUND_NO_ERROR_INFO_PROMPT = """You are an expert in identifying and resolving security vulnerabilities \
and code violations, with a deep understanding of java code analysis.
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

BACKGROUND_WITH_ERROR_INFO_PROMPT = """You are an expert in identifying and resolving security vulnerabilities \
and code violations, with a deep understanding of java code analysis.
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

Here is error information about this code modification, \
which provides details on issues such as security vulnerabilities, bug fixes, and more:
```
{error_info}
```

Your task is to:
Step 1 - Identify code modifications related to the error information provided to you: \
Understand the purpose and significance of the changes within the context of code.
Step 2 - Identify the violation related to the error information provided to you addressed.
"""