"""
DSL优化框架的Prompt模板
"""

# DSL语法说明（详细版）
DSL_GRAMMAR_PROMPT = """
DSL Grammar Overview (BNF):

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

**Detailed Lexical and Syntax Rules:**

1. **Keywords** (case-insensitive): where, not, and, or, match, is, contain, in

2. **Operators**: 
   - Comparison: ==, !=
   - Property access: .
   - Separators: , (comma), ; (semicolon)
   - Grouping: ( )

3. **Identifiers**:
   - Must start with letter (a-z, A-Z) or underscore (_)
   - Can contain letters, digits (0-9), and underscores
   - Examples: functionCall, _node, node1, catchBlock_1
   - Invalid: 1node (cannot start with digit), node-name (cannot contain hyphen)

4. **String Literals**:
   - Support single quotes '...' and double quotes "..."
   - Support escape sequences: \\", \\', \\\\, \\n, \\t, \\r, \\b, \\f, \\uXXXX
   - Examples: "hello", 'world', "say \\"hello\\"", 'it\\'s ok'

5. **Number Literals**:
   - Integers: 0, 123, -45
   - Floats: 3.14, -0.5
   - Scientific notation: 1e10, 2.5e-3

6. **Boolean Literals**: true, false

7. **Whitespace**: Spaces, tabs, newlines are ignored

---

**Syntax Rules and Examples:**

1. **Query Structure**:
   - MUST end with semicolon (;)
   - MUST have "where" keyword
   - Format: NodeType [Alias] where Condition ;
   
   Example:
   ```dsl
   functionCall fc where fc.name == "test" ;
   ```

2. **Entity Declaration**:
   - NodeType is required (e.g., functionCall, ifBlock)
   - Alias is optional but recommended for referencing in conditions
   
   Examples:
   ```dsl
   functionCall          // No alias
   functionCall fc       // Alias: fc
   ```

3. **Conditions**:
   - Atomic: Simple value or relation match
   - Logical NOT: not (Condition)
   - Logical AND: and (Condition, Condition, ...)  // At least 2 conditions
   - Logical OR: or (Condition, Condition, ...)    // At least 2 conditions
   
   Examples:
   ```dsl
   // Atomic
   fc.name == "test"
   
   // NOT
   not(fc.name == "test")
   
   // AND
   and(
       fc.name == "test",
       fc.arguments != null
   )
   
   // OR
   or(
       fc.name == "test1",
       fc.name == "test2"
   )
   ```

4. **Value Match**:
   - Format: Attribute ValueComp Value
   - ValueComp: match (regex), is (type check), ==, !=
   
   Examples:
   ```dsl
   fc.name == "test"
   fc.name match "test.*"
   fc.returnType is functionCall
   ```

5. **Relation Match** (Nested Query):
   - Format: Attribute NodeComp Query
   - NodeComp: contain, in
   - Query MUST be complete (with semicolon)
   
   Example:
   ```dsl
   fd.body contain functionCall fc where fc.name == "helper" ;
   ```

6. **Attribute Path**:
   - Format: Alias (.Property)*
   - Must start with alias (cannot start with dot)
   - Can have multiple property levels
   
   Examples:
   ```dsl
   fc                    // Simple alias
   fc.name               // One level
   fc.body.arguments     // Multiple levels
   ```

7. **Value Types**:
   - NodeType identifier
   - Variable identifier
   - String literal (with quotes)
   - Number literal
   - Boolean literal (true/false)

---

**Common Syntax Errors to Avoid:**

1. ❌ Missing semicolon:
   ```dsl
   functionCall where fc.name == "test"  // Missing ;
   ```
   ✅ Correct:
   ```dsl
   functionCall where fc.name == "test" ;
   ```

2. ❌ Attribute path without alias:
   ```dsl
   functionCall where .name == "test" ;  // Missing alias
   ```
   ✅ Correct:
   ```dsl
   functionCall fc where fc.name == "test" ;
   ```

3. ❌ Nested query without semicolon:
   ```dsl
   fd.body contain functionCall fc where fc.name == "helper"  // Missing ;
   ```
   ✅ Correct:
   ```dsl
   fd.body contain functionCall fc where fc.name == "helper" ;
   ```

4. ❌ and/or with single condition:
   ```dsl
   and(fc.name == "test")  // Need at least 2 conditions
   ```
   ✅ Correct:
   ```dsl
   and(
       fc.name == "test",
       fc.arguments != null
   )
   ```

5. ❌ Unmatched quotes:
   ```dsl
   fc.name == "test'  // Quote mismatch
   ```
   ✅ Correct:
   ```dsl
   fc.name == "test"
   ```

---

**Complete Examples:**

Example 1 - Simple query:
```dsl
functionCall fc where fc.name == "test" ;
```

Example 2 - Nested query:
```dsl
functionDeclaration fd where
    fd.body contain functionCall fc where
        fc.name == "helper"
    ;
```

Example 3 - Complex conditions:
```dsl
catchBlock cb where
    and(
        cb.body contain binaryOperation bo where
            bo.operator == "instanceof",
        cb.parameters contain valueDeclaration vd where
            vd.type == "Exception"
    )
;
```

Example 4 - Regex matching:
```dsl
functionCall fc where
    fc.name match "(?i).*(utils|helper|utility)$"
;
```

Example 5 - Logical combinations:
```dsl
ifBlock ifb where
    and(
        ifb.condition != null,
        or(
            ifb.body contain functionCall,
            ifb.body contain objectCreationExpression
        )
    )
;
```

---

**Important Constraints for LLM Generation:**

1. ✅ ALWAYS end queries with semicolon (;)
2. ✅ Use aliases to reference nodes in conditions (e.g., fc.name, not functionCall.name)
3. ✅ Wrap logical operations in parentheses: and(...), or(...), not(...)
4. ✅ Attribute paths must start with alias, not with dot
5. ✅ Escape quotes in strings: use \\" or \\'
6. ✅ Nested queries must be complete (with semicolon)
7. ✅ and/or require at least 2 conditions
8. ✅ Match operator expects regex pattern in string literal
"""

# Step1: 分析DSL数据
ANALYZE_DSL_PROMPT = """
You are given a DSL query that attempts to detect a specific type of bug. This DSL was generated based on the buggy code and fixed code pair, with the goal of matching code that exhibits the root cause of the defect.

IMPORTANT PRINCIPLES:
- The DSL MUST match the buggy_code (it should detect the defect pattern)
- The DSL MUST NOT match the fixed_code (it should not match the corrected code)
- The DSL should ideally capture the essential characteristics described by the root cause

Your task is to analyze the DSL query and understand:
1. What AST nodes and their properties the DSL is trying to match
2. What conditions are being checked
3. The structure and logic of the DSL query
4. Whether the DSL comprehensively captures the root cause or only partially describes the buggy code structure

{dsl_grammar}

---

Here is the DSL query:
```dsl
{dsl_code}
```

Here is the buggy code (the DSL should match this):
```java
{buggy_code}
```

Here is the fixed code (the DSL should NOT match this):
```java
{fixed_code}
```

Description of the root cause:
{root_cause}

---

Please analyze the DSL query structure and explain:
1. What entity types are being queried
2. What conditions are applied to each entity
3. How the conditions are combined (and/or/not)
4. What properties and values are being checked
5. Whether the DSL comprehensively captures the root cause description, or only partially describes the buggy code structure
6. What essential characteristics from the root cause might be missing in the current DSL

Output your analysis in the following format:
[DSL_ANALYSIS]
<your detailed analysis here>
[/DSL_ANALYSIS]
"""

# Step2: 分析FP原因
ANALYZE_FP_PROMPT = """
Based on the previous DSL analysis, you found a false positive (FP) - code that the DSL incorrectly matches but should not.

Here is the FP code:
```java
{fp_code}
```

Your task is to explain in natural language what needs to be changed in the DSL to avoid matching this FP code.

Think about:
1. **Which DSL node/condition is causing the false match?**
   - For example: "The node named 'funcCall' matches any function call, but it should only match calls to specific methods"
   - Or: "The condition 'funcCall.arguments == null' is too broad, it matches cases where arguments exist but are empty"

2. **What should be different?**
   - For example: "The node name should be more specific, like 'funcCall' should become 'funcCall where funcCall.name == \"specificMethod\"'"
   - Or: "We need to check not just if arguments exist, but also if they contain specific values"

3. **What's the root cause?**
   - Scenario 1: The DSL is too general - it correctly describes the pattern but matches too many cases. We need to add more specific conditions.
   - Scenario 2: The DSL is incomplete - it's missing important conditions from the buggy code that would distinguish real defects from FPs.

Output your analysis in natural language:
[FP_ANALYSIS]
Scenario: <1 or 2>
Reasoning: <explain in plain language why this scenario applies>

What needs to change:
<describe in natural language what specific DSL nodes/conditions need to be modified or added>
- Example: "The 'funcCall' node should check that the function name matches 'specificMethod'"
- Example: "We need to add a condition that 'funcCall.arguments' contains at least one element"

Key differences between FP and buggy code:
<describe what makes the FP code different from the actual buggy code>
[/FP_ANALYSIS]
"""

# Step3: 提取额外约束
EXTRACT_CONSTRAINT_PROMPT = """
Based on the previous FP analysis, extract the specific constraints that need to be modified in the DSL.

**IMPORTANT: You must base your constraints on the EXISTING nodes in the original DSL.**
- Only add constraints to nodes that already exist in the original DSL
- Use the exact node aliases from the original DSL
- Do NOT create new node structures or modify contain/in sub-queries in ways that change the node structure
- Focus on adding simple attribute constraints to existing nodes

Original DSL:
```dsl
{original_dsl}
```

Source code to analyze:
```java
{source_code}
```

Source type: {source_type} (buggy/fixed/fp)

---

**What to extract:**

For each constraint modification you find, specify:

1. **Type**: The type of modification needed
   - "add": Add a new constraint (the constraint doesn't exist in the original DSL)
   - "edit": Modify an existing constraint's value (the constraint exists but needs a different value)
   - "del": Remove an existing constraint (the constraint should be deleted from the DSL)
   
2. **Constraint Path**: Where the constraint is located, in format "nodeAlias.attribute"
   - **CRITICAL**: The nodeAlias MUST be an existing node alias from the original DSL above
   - Example: "funcCall.name" means the "name" attribute of the node with alias "funcCall" (if "funcCall" exists in original DSL)
   - Example: "ifBlock.condition" means the "condition" attribute of the "ifBlock" node (if "ifBlock" exists in original DSL)
   - **For sub-query constraints**: Use the alias of the node defined in the contain/in sub-query directly
     - Example: If original DSL has "body contain valueDeclaration vd", use "vd.name" to add constraint to the sub-query node
     - The nodeAlias must match an existing alias in the original DSL (e.g., "vd", "param", etc.)
   - **DO NOT** create new node aliases or modify the node structure
   - For "edit" and "del" types, this must match an existing constraint path in the original DSL
   - For "add" type, the nodeAlias must exist in the original DSL, and you're adding a new attribute constraint to that existing node
   
3. **Operator**: How to compare (required for "add" and "edit" types)
   - For simple values: "==", "!=", "match", "is"
   - For checking if something contains another node: "contain", "in"
   - For "del" type, this field can be omitted or left empty
   
4. **Value**: What to compare against (required for "add" and "edit" types)
   - For simple values: the actual value (e.g., "specificMethod", "null", "true")
   - For sub-queries: the complete DSL sub-query (e.g., "functionCall where functionCall.name == \"method\" ;")
   - For "edit" type: this is the NEW value that should replace the existing one
   - For "del" type, this field can be omitted or left empty
   
5. **Original Value**: (Optional, only for "edit" type)
   - The original value of the constraint that needs to be modified
   - This helps locate the exact constraint to edit when multiple constraints exist at the same path
   - If omitted, the first matching constraint at the path will be edited
   
6. **Is Negative**: "yes" if this constraint should filter out FP (use not()), "no" if it should be added as a positive condition
   - For "del" type, this field can be omitted

**Output Format:**
[CONSTRAINTS]
Constraint 1:
- Type: add
- Path: funcCall.name
- Operator: ==
- Value: specificMethod
- Is Negative: no

Constraint 2:
- Type: edit
- Path: funcCall.arguments
- Operator: ==
- Original Value: null
- Value: empty
- Is Negative: no

Constraint 3:
- Type: del
- Path: ifBlock.condition
- Is Negative: no

Constraint 4:
- Type: add
- Path: funcCall.arguments
- Operator: contain
- Value: functionCall where functionCall.name == "helper" ;
- Is Negative: yes

[/CONSTRAINTS]

**Important:**
- **CRITICAL RULE**: All node aliases in Path MUST exist in the original DSL above. Do NOT create new node structures.
- Path format: "nodeAlias.attribute" (use the EXACT alias from the original DSL)
  - For main query nodes: use the main node alias from original DSL (e.g., if original has "functionDeclaration fd", use "fd.name")
  - For sub-query nodes: use the sub-query node alias from original DSL (e.g., if original has "body contain valueDeclaration vd", use "vd.name")
  - **Always verify the nodeAlias exists in the original DSL before using it**
- Type must be one of: "add", "edit", "del"
- For "add" type: Path, Operator, and Value are required
  - **"add" means adding a new attribute constraint to an EXISTING node** (e.g., adding "operator is instanceof" to an existing "binaryoperation_1" node)
  - **DO NOT use "add" to create new node structures or modify contain/in sub-queries**
- For "edit" type: Path, Operator, and Value are required; Original Value is optional but recommended
- For "del" type: Only Path is required; Operator and Value can be omitted
- **Avoid complex sub-query modifications**: Prefer adding simple attribute constraints to existing nodes
- Keep it simple and clear - one constraint per block
- If no constraints found, output: [CONSTRAINTS]\n[/CONSTRAINTS]

**Examples for sub-query constraints:**
- Original DSL: `functionDeclaration fd where fd.body contain valueDeclaration vd;`
- To add constraint to sub-query: Path should be "vd.name" (using the sub-query alias "vd" directly)
- Original DSL: `functionCall fc where fc.arguments contain param where param.type == "String";`
- To edit constraint in sub-query: Path should be "param.type" (using the sub-query alias "param" directly)

**Format Variations Accepted:**
The following format variations are also acceptable:
- Field names can be separated by colons or dashes: "Path:", "Path -", "Type:", "Type -"
- Field order can vary
- Multiple constraints can be separated by blank lines or "Constraint N:" markers
"""

# Step3.5: 验证和修复约束
VALIDATE_CONSTRAINT_PROMPT = """
You previously extracted constraints to modify the DSL. However, some of these constraints have syntax or semantic errors.

**Original DSL:**
```dsl
{original_dsl}
```

**Extracted Constraints (with errors):**
{constraints_json}

**Validation Errors:**
{validation_errors}

**Fix Suggestions:**
{fix_suggestions}

---

**Your Task:**
Fix the invalid constraints based on the validation errors and fix suggestions. You must output the corrected constraints in the same format as before.

**Important Rules:**
1. **Only fix the constraints that have errors** - keep valid constraints unchanged
2. **Follow DSL syntax rules strictly** - use the DSL grammar provided earlier
3. **Node types and properties must be valid** - refer to the validation errors for details
4. **For operator property issues**: If you see errors like "Property 'operator' is not supported", use node type check instead
   - Wrong: `binaryoperation_1.operator is instanceof`
   - Correct: `binaryoperation_1 is instanceofExpression`
5. **Maintain the same constraint structure** - only change the invalid parts

**Output Format:**
[CONSTRAINTS]
Constraint 1:
- Type: {type}
- Path: {fixed_path}
- Operator: {fixed_operator}
- Value: {fixed_value}
- Is Negative: {is_negative}

Constraint 2:
...
[/CONSTRAINTS]

**Note:** Output ALL constraints (both fixed and unchanged) in the same order, but with corrections applied to invalid ones.
"""

# Step4已移除，改为基于约束手动编辑DSL，不再使用LLM
