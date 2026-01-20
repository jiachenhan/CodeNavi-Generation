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

A False Positive means the DSL's constraints are NOT strict enough. We need to add more constraints to exclude the FP.

**Scenario 1: DSL is too broad**
- The constraints extracted from buggy code are also satisfied by FP code
- FP shares some characteristics with buggy code, but lacks the KEY defect pattern
- **Solution**: Add constraints that FP does NOT satisfy (to exclude FP)

**Scenario 2: Key constraints from buggy code were NOT extracted**
- The original buggy code has important characteristics that were missed during extraction
- The DSL cannot fully capture the defect's root cause, causing over-matching
- **Solution**: Go back to buggy code and extract the MISSING constraints (even if they don't directly describe the root cause)

---

**Your Task:**

1. **Compare FP code with buggy code** - What's the KEY difference?
2. **Identify the scenario** - Is it Scenario 1 (need to exclude FP) or Scenario 2 (missed buggy code features)?
3. **Propose constraints** - What constraints should be added to prevent matching FP?

**Output format:**
[FP_ANALYSIS]
Scenario: <1 or 2>

Reasoning:
<Explain which scenario applies and why>

Key differences between FP and buggy code:
<List the critical differences that should distinguish them>

What constraints to add:
<Describe what constraints need to be added to the DSL>
- If Scenario 1: Constraints that FP does NOT satisfy
- If Scenario 2: Constraints from buggy code that were missed

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

**Constraint Extraction Rules:**

1. **Type**: add / edit / del
   - add: Add new constraint to EXISTING node
   - edit: Change existing constraint value
   - del: Remove constraint

2. **Path**: nodeAlias.property OR nodeAlias (for type check)
   - MUST use existing alias from original DSL above
   - Examples: binaryoperation_1, functioncall_1.name, catchblock_1.parameters

3. **Operator**: ==, !=, match, is
   - Use 'is' for type checking
   - Use 'match' for regex patterns

4. **Value**: The constraint value
   - For type check: NodeType name (e.g., "instanceofExpression")
   - For property check: literal value or regex pattern
   - MUST be a valid value based on metadata above

---

**What NOT to Do (IMPORTANT):**

❌ BAD Example 1 - Creating new node:
Constraint:
- Type: add
- Path: catchblock_1.body
- Operator: contain
- Value: binaryOperation newnode where newnode is instanceofExpression;

Why bad: This creates a NEW node structure, violating the rules.

❌ BAD Example 2 - Using non-existent property:
Constraint:
- Type: add
- Path: binaryoperation_1.operator
- Operator: match
- Value: instanceof

Why bad: Property 'operator' doesn't exist on binaryOperation (check metadata).

❌ BAD Example 3 - Duplicate constraint:
If original DSL already has: binaryoperation_1 is instanceofExpression
Don't add:
- Type: add
- Path: binaryoperation_1
- Operator: is
- Value: instanceofExpression

Why bad: This constraint already exists in the original DSL. Check the original DSL carefully before adding constraints.

❌ BAD Example 4 - Reusing existing alias in new contain clause:
If original DSL already has: catchblock_1.body contain binaryOperation binaryoperation_1 where ...
Don't add:
- Type: add
- Path: catchblock_1.body
- Operator: contain
- Value: binaryOperation binaryoperation_1 where binaryoperation_1.lhs != null;

Why bad: This reuses the alias 'binaryoperation_1' which already exists. If you want to add constraints to binaryoperation_1, directly use:
- Path: binaryoperation_1.lhs
- Operator: !=
- Value: null

❌ BAD Example 5 - Overfitting to FP by creating new contain clause:
If original DSL has: binaryoperation_1 contain functionCall functioncall_1 where functioncall_1.base.name match "(?i).*(utils|helper)$"
And FP code calls: CloseUtil.closeQuietly(...)
Don't add:
- Type: add
- Path: catchblock_1.body
- Operator: contain
- Value: functionCall functioncall_2 where not(functioncall_2.base.name match "(?i).*CloseUtil.*")

Why bad:
1. 这创建了一个带有新别名functioncall_2的新contain子句（违反规则：只能修改现有节点）
2. 这过拟合到特定的FP示例 - 排除"CloseUtil"不会帮助处理其他FP模式
3. 现有的functioncall_1约束已经泛化地处理了helper方法
4. 应该让现有约束更精确地匹配bug pattern，而不是为每个FP添加负向过滤器

❌ BAD Example 6 - Adding conflicting constraint to same property:
If original DSL has: functioncall_1.base.name match "(?i).*(utils|helper)$"
Don't add:
- Type: add
- Path: functioncall_1.base.name
- Operator: match
- Value: "(?i).*CloseUtil.*"

Why bad:
1. functioncall_1.base.name已经有一个match约束了
2. 同一个属性不能同时匹配两个不同的regex模式
3. 如果需要修改现有约束，应该使用Type: edit而不是add

---

**GOOD Examples:**

✅ Add type check to existing node:
Constraint:
- Type: add
- Path: binaryoperation_1
- Operator: is
- Value: instanceofExpression

✅ Add property constraint:
Constraint:
- Type: add
- Path: functioncall_1.name
- Operator: !=
- Value: "helper"

---

**Output Format:**
[CONSTRAINTS]
Constraint 1:
- Type: add
- Path: <existing_alias or existing_alias.property>
- Operator: <==|!=|match|is>
- Value: <value>

Constraint 2:
...
[/CONSTRAINTS]

**Remember:**
- Only modify EXISTING nodes - use aliases that are already defined in the original DSL
- Check metadata for valid properties
- Check original DSL to avoid duplicate constraints
- If a constraint already exists in original DSL, don't add it again
- NEVER reuse an existing alias in a new contain/in clause - this creates alias conflicts
- If you want to add constraints to an existing node, use its alias directly in Path field
"""


# ============================================================================
# Step 3.5: 验证和修复约束
# ============================================================================

VALIDATE_CONSTRAINT_PROMPT = """
Your previous constraints have validation errors. Fix them based on the errors and suggestions.

**Original DSL:**
```dsl
{original_dsl}
```

**Your Constraints (with errors):**
{constraints_json}

**Validation Errors:**
{validation_errors}

**Fix Suggestions:**
{fix_suggestions}

---

**Your Task:**

1. **Follow the Fix Suggestions** - Each error has a specific fix suggestion provided above
2. **Only fix constraints with errors** - Keep valid constraints unchanged
3. **Maintain constraint structure** - Only change the invalid parts based on suggestions

**Output Format:**
[CONSTRAINTS]
Constraint 1:
- Type: add
- Path: <corrected_path>
- Operator: <corrected_operator>
- Value: <corrected_value>
...
[/CONSTRAINTS]

Output ALL constraints (both fixed and unchanged) in order.
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
