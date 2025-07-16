TASK_DESCRIPTION_PROMPT = """I hope to summarize the violations in the code into defect templates, and the template \
can be used to detect similar violation in other codebase. 

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
    functionCall f1 where f1.name match "(?i).*set(Accessible|Visiable)";
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

Note: A code line is critical if it is part of a defective code snippet.
Note: If this kind of violation occurs more than once, you only need to keep the line numbers \
of one defective code fragment and ignore the other defective code lines. \
Because the template will be accurate if only use one code snippet.

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

IDENTIFY_ELEMENTS_PROMPT = """Based on your analysis, Please select which elements are critical for this violation. 
Your task is \
1. Decompose the defective version code into Abstract Syntax Tree (AST).
2. Then mark the key elements in the AST according to the defect template that you analyzed.
3. Output results in the following strict format:

The following JSON list comprises elements pre-identified as likely significant - please reference this during your selection process.
###JSON_LIST
{Genpat_Json_info}

### OUTPUT FORMAT
[CRITICAL_ELEMENTS_START]
@@ element_type @@ element_value @@ line_number
[CRITICAL_ELEMENTS_END]

### RULES
1. Use standard AST node types (e.g., "TryStatement", "CatchClause", "WhileStatement")
2. Each element must be on a separate line using @@ as separators
3. element_value must be exact substring of the original code
4. line_number is the starting line of the element in the code

### EXAMPLE CODE
4: try {
5:   Files.newReader(asciiFile, null);
6:   fail("expected exception");
7: } catch (NullPointerException e) {
8: }

### EXAMPLE OUTPUT
[CRITICAL_ELEMENTS_START]
@@ TryStatement @@ try {...} catch (...) {...} @@ 4
@@ CatchClause @@ catch (NullPointerException e) @@ 7
@@ SimpleType @@ NullPointerException @@ 7
@@ MethodInvocation @@ Files.newReader(asciiFile, null) @@ 5
[CRITICAL_ELEMENTS_END]
"""
