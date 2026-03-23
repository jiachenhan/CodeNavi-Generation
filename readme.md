# Bug Pattern Detection: Automated Construction and Refinement

## 📝 Overview
This repository contains the implementation of two complementary techniques for bug pattern detection:

### **DSLGEN: Automated Bug Pattern Construction from Code Modifications**
Addresses the challenge of automatically generating reusable detection rules from individual bug-fix examples. DSLGEN constructs a **modification-associated graph** that performs differential analysis on pre- and post-fix code, organizing edit operations and syntactic structures from both sides to provide a complete foundation for semantic analysis. Building on this, it introduces a **repair-intent-driven semantic analysis mechanism** that infers root causes from repair intentions, identifies defect-related elements, and extracts constraints to transform them into DSL detection patterns.

<p align="center">
  <img src="09appendix/overview-dslgen.svg" width="700" alt="DSLGEN Overview">
</p>

### **DSLRefiner: Pattern Refinement via Counterexample Feedback**
Addresses the iterative optimization challenge when rules produce false positives during actual deployment. DSLRefiner is applicable to detection rules from any source (automatically generated or manually written). When false positives occur, it analyzes semantic differences between false-positive instances and correct matches, identifies detection conditions requiring adjustment, performs targeted pattern modifications, and ensures the refined rules do not miss truly problematic code through controlled constraint update strategies.

<p align="center">
  <img src="09appendix/overview-dslrefiner.pdf" width="700" alt="DSLRefiner Overview">
</p>

## 📂 Repository Structure

```text
DSLGEN/
├── 01pattern/              # Intermediate results about transfer-graph-based pattern
├── 02pattern-info/         # Intermediate results about Metadata for patterns and LLM's result
├── 06config/
│   └── config.yml          # Configuration file
├── 07dsl/                  # Extracted DSL schema
├── 08example/              # Example input bug-fix case
├── 09appendix/             # Supplementary materials (e.g., User Study)
│
├── ModifiedMetaModel/      # Java implementation of core framework
│   ├── repair.ast/         # AST node modeling and traversal
│   ├── repair.dsl/         # DSL query translation
│   ├── repair.pattern/     # Transfer-graph
│   └── repair.main/        # Entry
│
├── script/
│   ├── app/                # Python implementation of core framework
│   ├── exp/                # Experiment and evaluation scripts
│   └── requirements.txt    # Python dependencies
│
├── Utils/                  # Common utility functions
├── pom.xml                 # Java project build file (Maven)
└── README.md
```

## ⚙️ Environment Setup
1. Java (for transfer graph construction and dsl translation)
    * JDK version: Java 17 recommended
    * Build tool: Maven 3.6+

   To build the Java part:
    ``` bash
    mvn clean install
    ```
   This will generate `ModifiedMetaModel-1.0-SNAPSHOT-runnable.jar` in the `ModifiedMetaModel/artifacts/` directory.

2. Python (for scripting and experiments)
    * Python version: Python 3.11+

   Install dependencies:
    ``` bash
    cd script
    pip install -r requirements.txt
    ```
3. Configuration

   All runtime parameters (e.g., LLM endpoint and API key) can be configured in:
    ``` text
    06config/config.yml
    ```
   Please modify this file according to your LLM.

## ▶️ Usage

The full end-to-end pipeline has been integrated into a Python script.
You can run the entire workflow to extract a DSL pattern from a single bug-fix example:

``` bash
cd script
python -m app.pipeline.codepair
    --code_path /path/to/codepair  # e.g., ../08example/code
    --dsl_path /path/to/store/dsl
```
Make sure to configure runtime parameters in `06config/config.yml`


