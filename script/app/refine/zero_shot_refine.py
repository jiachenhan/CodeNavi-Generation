import json
import re
from pathlib import Path
from typing import Optional

import utils
from app.basic_modification_analysis import original_code, fixed_code
from app.communication import PatternInput
from interface.llm.llm_api import LLMAPI
from interface.llm.llm_openai import LLMOpenAI
from utils.common import retry_times, valid_with
from utils.config import set_config

ZERO_REFINE_PROMPTS = """
You are given a buggy Java code snippet and its corresponding fix. \
A textual description of the underlying bug is also provided.
Additionally, you are given a DSL query that attempts to describe the pattern of this bug, \
but it may contain irrelevant or overly specific constraints.

Your task is to revise the DSL query so that it accurately and minimally \
captures the essential characteristics of the bug, as revealed by the difference between the buggy and fixed code, \
and the bug description.

Remove any conditions that are unrelated to the bug or unnecessarily specific, \
but preserve all constraints that are essential to express the bug's semantics.

---

DSL Grammar Overview (Simplified BNF):

Query ::= EntityDecl where Condition ;  
EntityDecl ::= NodeType (Alias)?  
Condition ::= AtomicCond  
           | not (Condition)  
           | and (Condition (, Condition)+)  
           | or  (Condition (, Condition)+)  
AtomicCond ::= ValueMatch | RelMatch  
ValueMatch ::= Attribute ValueComp Value  
RelMatch   ::= Attribute NodeComp Query  
Attribute  ::= Alias (.Property)*  
Value      ::= NodeType | <var name> | literal value  
NodeType   ::= functionCall | ifBlock | objectCreationExpression | ...  
Property   ::= body | arguments | type | ...  
ValueComp  ::= match | is | == | !=  
NodeComp   ::= contain | in  
Alias      ::= any valid identifier

---

Please follow these strict syntax rules while revising the DSL:

1. All logical combinations must use nested blocks: `and(...)`, `or(...)`, or `not(...)`. 
   ❌ Do NOT use inline conjunctions like `cond1 and cond2`.
   ✅ Always use: `and(cond1, cond2)`

2. If the original DSL used `and(...)` or `or(...)` with multiple conditions, and your revision removes some of those conditions:
   - If there are still **two or more conditions** left, retain the `and(...)` or `or(...)` wrapper.
   - If there is only **one condition** left, **remove the `and(...)` or `or(...)` wrapper** and directly use the inner condition.

3. Do not change the DSL structure or syntax in any other way. Only delete unnecessary constraints while preserving correct syntax.
   
4. ❗ Do NOT invent new entities, aliases, types, or property paths. \
You are only allowed to **reuse the exact identifiers and structures** that appear in the original DSL.  
   ❌ Wrong: `typeUsage`, `typeusage_1`, or `someNewNode` if they don’t appear in the original DSL.  
   ✅ Only use entity types, alias names, and field accesses that exist in the original DSL.
   
5. You must preserve combinations of constraints that together express the **semantically meaningful aspects of the bug
❗ For example, if a condition identifies a specific deprecated constructor like `new Integer(...)`, \
then both the constructor kind and the type `Integer` are essential and **must be preserved together.  
   Removing only one part will lead to loss of critical meaning, and must be avoided.
   ✅ For example, if the DSL originally contains:
   ```dsl
       and(
          alias_1 is objectCreationExpression,
          alias_1.type.name match ".*Integer"
        )
   ```
   and the bug is about using new Integer(...), then both parts are essential and must be preserved together. \
   ❌ Removing alias_1.type.name match ".*Integer" alone will lose the ability to recognize the bug pattern.

6. Every top-level query must end with a semicolon (;).
❗ This is a mandatory syntax rule.
❌ Do not omit the semicolon at the end of the optimized DSL.
   

### TASK ###
Here is the buggy code:
{buggy_code}

Here is the fixed code:
{fixed_code}

Description about the violation in the buggy code :
{bug_description}

Original DSL:
{origin_dsl}


Please provide a revised DSL and output the optimized DSL using the following format:
[OPTIMIZED_DSL]
<your optimized DSL here>
[/OPTIMIZED_DSL]
"""


class Refiner:
    def __init__(self,
                 llm: LLMAPI,
                 pattern_input: PatternInput,
                 log_path: Path,
                 retries: int = 5):
        self.llm = llm
        self.pattern_input = pattern_input
        self.log_path = log_path
        self.retries = retries

    pattern = re.compile(r"""
        \[OPTIMIZED_DSL]
        (.*?)
        \[/OPTIMIZED_DSL] 
        """, re.DOTALL | re.VERBOSE  # DOTALL: .匹配换行；VERBOSE: 允许注释和空白
    )

    def refine(self, origin_dsl_path: str) -> str:
        prompt = ZERO_REFINE_PROMPTS.format(buggy_code=original_code(self.pattern_input),
                                            fixed_code=fixed_code(self.pattern_input),
                                            bug_description=self.pattern_input.error_info,
                                            origin_dsl=origin_dsl_path)
        task_messages = [{"role": "user", "content": prompt}]
        valid, response = self.invoke_validate_retry(task_messages)
        if valid:
            task_messages.append({"role": "assistant", "content": response})
            refined_dsl = self.extract_optimized_dsl(response)
            return refined_dsl

    def check_valid(self, response: str) -> bool:
        match = re.search(self.pattern, response)
        return bool(match)

    @retry_times(retries=5)
    @valid_with(check_valid)
    def invoke_validate_retry(self, messages: list) -> str:
        return self.llm.invoke(messages)

    def extract_optimized_dsl(self, response) -> Optional[str]:
        match = re.search(self.pattern, response)
        if match:
            optimized_dsl = match.group(1).strip()
            return optimized_dsl


if __name__ == "__main__":
    config = set_config("yunwu")
    jar_path = config.get("jar_path")
    model_name = config.get("openai").get("model")

    llm = LLMOpenAI(base_url=config.get("openai").get("base_url"),
                    api_key=config.get("openai").get("api_keys")[0],
                    model_name=model_name)

    dataset_name = "ql"
    checker_name = "Deprecated_method_or_constructor_invocation"
    group_name = "0"
    case_name = "1"

    case_path = Path("E:/dataset/Navi/DEFs") / dataset_name / checker_name / group_name / case_name

    pattern_info_input_path = (utils.config.get_pattern_info_base_path() / model_name / dataset_name / "input"
                               / checker_name / group_name / f"{case_name}.json")
    dsl_path = (utils.config.get_dsl_base_path() / model_name / dataset_name
                / checker_name / group_name / f"{case_name}.kirin")
    log_path = utils.config.get_dsl_base_path() / model_name / dataset_name / checker_name / group_name / "refine.log"

    case_info = json.load(open(case_path / "info.json", 'r'))["may_be_fixed_violations"].strip()
    pattern_input = PatternInput.from_file(pattern_info_input_path)
    pattern_input.set_error_info(case_info)

    refiner = Refiner(llm, pattern_input, log_path)

    with open(dsl_path, "r") as f:
        ori_dsl = f.read()

    refined_dsl = refiner.refine(ori_dsl)
    print(refined_dsl)

