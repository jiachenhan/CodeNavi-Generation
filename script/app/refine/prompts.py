"""
DSL优化框架的Prompt模板

设计原则:
1. 只提供约束层面的知识,不讲完整DSL语法
2. 示例必须是约束片段,不是完整Query
3. 明确提供节点类型-属性元数据
4. 简洁清晰,每个prompt控制在合理长度
"""

# ============================================================================
# 元数据生成函数
# ============================================================================

def get_node_metadata_prompt() -> str:
    """
    生成节点类型和属性的元数据prompt

    这是最关键的信息,告诉LLM:
    - 哪些节点类型存在
    - 每个节点有哪些属性
    - 属性的类型(Collection/Body/Child/Simple)决定如何访问
    """
    from app.refine.parser.dsl_metadata import (
        OPERATOR_TO_NODE_TYPE
    )

    # 常用节点类型及其属性
    common_nodes = {
        "functionCall": ["name", "base", "arguments"],
        "functionDeclaration": ["name", "returnType", "parameters", "body", "exceptionTypes"],
        "ifBlock": ["condition", "thenBlock", "elseBlock"],
        "whileBlock": ["condition", "body"],
        "forBlock": ["initialization", "condition", "iteration", "body"],
        "catchBlock": ["parameters", "body"],
        "binaryOperation": ["lhs", "rhs"],  # 注意: 没有operator属性!
        "instanceofExpression": ["lhs", "rhs"],
        "valueDeclaration": ["name", "type", "initializer"],
        "objectCreationExpression": ["type", "arguments"],
        "throwStatement": ["operand"],
        "returnStatement": ["returnValue"],
    }

    lines = ["**Node Types and Their Properties:**\n"]
    for node_type, properties in sorted(common_nodes.items()):
        lines.append(f"  {node_type}: {', '.join(properties)}")

    # 属性类型分类 - 这是关键!
    lines.append("\n**CRITICAL: Property Access Rules:**")
    lines.append("\n1. Collection Properties (contain multiple nodes):")
    lines.append("   - arguments, parameters, dimensions, elements, generics, throwExceptionTypes")
    lines.append("   - ❌ CANNOT use: catchblock_1.parameters.type")
    lines.append("   - ✅ MUST use: catchblock_1.parameters contain valueDeclaration where ...")

    lines.append("\n2. Body Properties (contain statements):")
    lines.append("   - body, thenBlock, elseBlock, tryBlock, catchBlocks, finallyBlock")
    lines.append("   - ❌ CANNOT use: catchblock_1.body.name")
    lines.append("   - ✅ MUST use: catchblock_1.body contain binaryOperation where ...")

    lines.append("\n3. Child Properties (single node, can chain):")
    lines.append("   - type, condition, lhs, rhs, base, operand, returnValue")
    lines.append("   - ✅ CAN use: functioncall_1.base.name")
    lines.append("   - ✅ CAN use: binaryoperation_1.lhs != null")

    lines.append("\n4. Simple Properties (direct values):")
    lines.append("   - value, primitiveTypeCode")
    lines.append("   - ✅ CAN use: literal_1.value == \"test\"")

    # 特殊规则
    lines.append("\n**Special Rules:**")
    lines.append("  ❌ binaryOperation does NOT have 'operator' property")
    lines.append("  ✅ To check instanceof: use 'nodeAlias is instanceofExpression'")

    return "\n".join(lines)


# ============================================================================
# 精简的约束语法指南 (用于ExtractConstraint阶段)
# ============================================================================

CONSTRAINT_SYNTAX_GUIDE = """
**Your Task: Generate Simple Constraints**

You will add/edit/delete constraints on EXISTING nodes in the DSL.
DO NOT create new node structures or complete DSL queries.

**Constraint Types:**

1. **Attribute value check (for Child/Simple properties):**
   Format: nodeAlias.property == value
   Example: functioncall_1.name == "helper"
   Example: binaryoperation_1.lhs != null

   ❌ WRONG: catchblock_1.parameters.type (parameters is Collection!)
   ❌ WRONG: catchblock_1.body.name (body is Body type!)

2. **Node type check:**
   Format: nodeAlias is NodeType
   Example: binaryoperation_1 is instanceofExpression
   Example: valuedeclaration_1 is valueDeclaration

**Valid Operators:**
- == : equals
- != : not equals
- match : regex pattern matching (use with string literals)
- is : type check

**Critical Rules:**
✅ Use EXISTING node aliases from the original DSL
✅ Only add constraints to nodes that already exist
✅ Use 'is' for type checking, not property access
✅ Collection/Body properties CANNOT be accessed with "." - they need contain/in queries
✅ Only Child/Simple properties can be chained with "."

❌ Don't create new node structures
❌ Don't modify contain/in sub-queries
❌ Don't use properties that don't exist on the node type
❌ Don't chain after Collection/Body properties (parameters, arguments, body, etc.)
"""


# ============================================================================
# Step 1: 分析DSL (精简版)
# ============================================================================

ANALYZE_DSL_PROMPT = """
You are analyzing a DSL query that detects a specific bug pattern.

**DSL Format (Brief):**
A DSL query has: NodeType alias where Condition ;

Conditions can check:
- Properties: alias.property == value
- Node types: alias is NodeType
- Logical ops: and(...), or(...), not(...)
- Nested queries: alias.property contain NodeType where ...

---

**DSL Query:**
```dsl
{dsl_code}
```

**Buggy Code (DSL should match this):**
```java
{buggy_code}
```

**Fixed Code (DSL should NOT match this):**
```java
{fixed_code}
```

**Root Cause:**
{root_cause}

---

**Your Task:**
Analyze whether the DSL comprehensively captures the root cause.

1. What nodes and properties does the DSL check?
2. What conditions are applied?
3. Does the DSL capture the ESSENTIAL characteristics of the root cause?
4. What might be missing?

Output format:
[DSL_ANALYSIS]
<your analysis>
[/DSL_ANALYSIS]
"""


# ============================================================================
# Step 2: 分析FP原因
# ============================================================================

ANALYZE_FP_PROMPT = """
Based on your DSL analysis, you found a **False Positive (FP)** - code that the DSL incorrectly matches but should NOT match.

**FP Code:**
```java
{fp_code}
```

**Understanding FP Scenarios:**

A False Positive means the DSL does NOT accurately capture the bug pattern. The DSL may be too broad OR missing key characteristics.

**Scenario 1: DSL is too broad**
- The DSL matches patterns that are NOT related to the root cause
- FP shares some characteristics with buggy code, but lacks the KEY defect pattern
- **Solution**: Add constraints to capture the ESSENTIAL bug pattern that FP does NOT have

**Scenario 2: Key characteristics from buggy code were NOT extracted**
- The original buggy code has important characteristics that were missed during extraction
- The DSL cannot fully capture the defect's root cause, causing over-matching
- **Solution**: Go back to buggy code and extract the MISSING constraints

---

**CRITICAL THINKING:**

Before proposing constraints, ask yourself:
1. **What is the CORE bug pattern described in the root cause?**
2. **Does the original DSL directly detect this CORE pattern?**
3. **If NOT, what parts of the original DSL are IRRELEVANT to the root cause?**

**Important:**
- If the original DSL contains constraints that are NOT related to the root cause, you should consider REMOVING them
- Don't just keep adding more constraints on top of irrelevant ones
- Focus on constraints that DIRECTLY capture the essence of the bug

---

**Your Task:**

1. **Identify the CORE bug pattern** from the root cause description
2. **Compare FP code with buggy code** - What's the KEY difference related to the CORE bug pattern?
3. **Analyze the original DSL** - Does it check for the CORE bug pattern? Or is it checking something else?
4. **Identify the scenario** - Is it Scenario 1 (DSL too broad) or Scenario 2 (missed key features)?
5. **Propose DSL modifications** - What should be changed?

**Output format:**
[FP_ANALYSIS]
Scenario: <1 or 2>

Core Bug Pattern:
<Describe the ESSENTIAL pattern that defines this bug, based on root cause>

Reasoning:
<Explain which scenario applies and why>

Key differences between FP and buggy code:
<List the critical differences that distinguish FP from buggy code>

Original DSL Issues:
<Analyze what's wrong with the original DSL - is it checking the right thing?>
- Does it detect the CORE bug pattern? If not, what is it detecting?
- Are there constraints unrelated to the root cause that should be removed?

Recommended Changes:
<Describe what should be changed in the DSL>
- Constraints to ADD (that capture the CORE bug pattern)
- Constraints to REMOVE (that are unrelated to the bug pattern)
- If Scenario 1: Focus on adding constraints that capture what FP is MISSING
- If Scenario 2: Focus on adding constraints from buggy code that were missed

[/FP_ANALYSIS]
"""


# ============================================================================
# Step 3: 提取约束 (核心阶段,重点优化)
# ============================================================================

EXTRACT_CONSTRAINT_PROMPT = """
Based on your FP analysis, extract specific constraints to modify the DSL.

{node_metadata}

{constraint_syntax}

---

**Original DSL:**
```dsl
{original_dsl}
```

**Source Code:**
```java
{source_code}
```

**Source Type:** {source_type}

---

**Constraint Format:**
- Type: add / edit / del
- Path: nodeAlias or nodeAlias.property (use EXISTING aliases from original DSL)
- Operator: == / != / match / is
- Value: The constraint value
- Is Negative: no (default) or yes (rare - only for explicit exclusion)

**Key Rules:**
1. Only modify EXISTING nodes - use aliases already defined in original DSL
2. To delete irrelevant constraints: Type: del
3. Always include "Is Negative: no" unless explicitly excluding something

---

**Common Mistakes:**

❌ Creating new nodes/aliases - Only modify EXISTING aliases from original DSL
❌ Using non-existent properties - Check metadata first (e.g., binaryOperation has no 'operator' property)
❌ Duplicate constraints - Check if constraint already exists in original DSL

---

**Examples:**

✅ Add constraint:
- Type: add
- Path: binaryoperation_1
- Operator: is
- Value: instanceofExpression
- Is Negative: no

✅ Delete irrelevant constraint:
- Type: del
- Path: functioncall_1.base.name
- Operator: N/A
- Value: N/A
- Is Negative: no

---

**Output Format:**
[CONSTRAINTS]
Constraint 1:
- Type: <add|edit|del>
- Path: <existing_alias or existing_alias.property>
- Operator: <==|!=|match|is|N/A>
- Value: <value|N/A>
- Is Negative: <no|yes>  // Use "no" for most cases. Only use "yes" for explicit exclusion.

Constraint 2:
...
[/CONSTRAINTS]

**Final Checklist:**
- ✅ Used EXISTING aliases from original DSL (not creating new ones)
- ✅ Checked metadata for valid properties
- ✅ Set "Is Negative: no" for all constraints (unless explicitly excluding)
- ✅ Verified no duplicate constraints
"""


# ============================================================================
# Step 3.5: 验证和修复约束
# ============================================================================

VALIDATE_CONSTRAINT_PROMPT = """
Some of your constraints have validation errors. Please fix them based on the error messages and suggestions below.

**Original DSL:**
```dsl
{original_dsl}
```

**Constraints with Errors:**

{fixable_constraints}

---

**Your Task:**

Fix the constraints listed above by following the "HOW TO FIX" suggestions for each error.

**Output Format:**
[CONSTRAINTS]
Constraint N:  // Output ONLY the fixed constraints (the ones with errors above)
- Type: <fixed_type>
- Path: <fixed_path>
- Operator: <fixed_operator>
- Value: <fixed_value>
- Is Negative: <no|yes>

Constraint M:
...
[/CONSTRAINTS]

**IMPORTANT:** Only output the constraints that had errors. Do NOT output valid constraints.
"""


# ============================================================================
# 动态生成完整prompt
# ============================================================================

def get_extract_constraint_prompt(original_dsl: str, source_code: str, source_type: str) -> str:
    """生成ExtractConstraint阶段的完整prompt"""
    return EXTRACT_CONSTRAINT_PROMPT.format(
        node_metadata=get_node_metadata_prompt(),
        constraint_syntax=CONSTRAINT_SYNTAX_GUIDE,
        original_dsl=original_dsl,
        source_code=source_code,
        source_type=source_type
    )
