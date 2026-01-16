# DSL优化框架

基于DSL+人工提供FP（False Positive，误报）的refine流程框架。

## 概述

该框架以LLM为核心推理组件，以规则化文本处理为执行组件，通过从FP中或原始代码对中没有注意到的程序元素中提取额外的DSL约束，增强DSL约束从而消除误报。

## 架构设计

### 数据结构

- **RefineInput**: 输入数据封装

  - `dsl_code`: DSL代码内容
  - `buggy_code`: 缺陷代码
  - `fixed_code`: 修复后的代码
  - `root_cause`: 缺陷原因描述
  - `fp_code`: 误报代码
- **ExtraConstraint**: 额外约束

  - `dsl_node`: 约束位置（DSL节点alias）
  - `constraint_type`: 约束类型（ValueMatch/RelMatch）
  - `attribute`: 属性路径
  - `value_comp`/`node_comp`: 比较操作符
  - `value`/`sub_query`: 值或子查询
  - `source_file`: 约束来源（buggy/fixed/fp）
- **LLMContext**: LLM对话上下文管理

  - 各步骤的对话历史
  - 中间结果存储

### 状态机流程

框架采用状态机模式，包含以下状态：

1. **InitialState** → **AnalyzeDSLState** (Step1: 分析DSL数据)
2. **AnalyzeDSLState** → **AnalyzeFPState** (Step2: 分析FP原因)
3. **AnalyzeFPState** → **ExtractConstraintState** (Step3: 提取额外约束)
4. **ExtractConstraintState** → **ConstructDSLState** (Step4: 构造最终DSL)
5. **ConstructDSLState** → **ExitState**

### 核心组件

- `dsl_refiner.py`: 主Refiner类，实现状态机逻辑
- `prompt_state.py`: 状态机状态类定义
- `prompts.py`: 各步骤的Prompt模板
- `data_structures.py`: 数据结构定义
- `dsl_constructor.py`: DSL构造辅助工具

## 使用方法

### 基本使用

```python
from app.refine import DSLRefiner, load_refine_input_from_paths
from interface.llm.llm_openai import LLMOpenAI
from pathlib import Path

# 初始化LLM
llm = LLMOpenAI(base_url="...", api_key="...", model_name="...")

# 加载输入数据
input_data = load_refine_input_from_paths(
    dsl_path=Path("path/to/dsl.kirin"),
    buggy_path=Path("path/to/buggy.java"),
    fixed_path=Path("path/to/fixed.java"),
    root_cause_path=Path("path/to/info.json"),
    fp_results_path=Path("path/to/fp_results.json")
)

# 创建优化器并执行
refiner = DSLRefiner(llm=llm, input_data=input_data)
refined_dsl = refiner.refine()

if refined_dsl:
    print(refined_dsl)
    # 保存上下文
    refiner.serialize_context(Path("output/context.json"))
```

### 输入文件格式

#### root_cause_path (info.json)

```json
{
  "may_be_fixed_violations": "缺陷原因描述..."
}
```

#### fp_results_path (labeled_results.json)

```json
[
  {
    "label": "fp",
    "method_source": "误报代码内容..."
  },
  ...
]
```

## 测试

运行测试文件：

```bash
python app/refine/test_refiner.py
```

测试文件会使用文档中指定的测试路径加载数据并执行优化流程。

## DSL语法

框架支持的DSL语法（BNF）：

```
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
```

## 设计特点

1. **状态机模式**: 清晰的流程控制，易于扩展和维护
2. **上下文管理**: 完整的对话历史记录，支持调试和回溯
3. **模块化设计**: 各组件职责明确，便于测试和修改
4. **LLM驱动**: 利用LLM的推理能力进行DSL分析和约束提取

## 文件结构

```
app/refine/
├── __init__.py           # 模块导出
├── doc.md                # 需求文档
├── README.md             # 本文档
├── data_structures.py    # 数据结构定义
├── prompts.py            # Prompt模板
├── prompt_state.py       # 状态机实现
├── dsl_refiner.py        # 主Refiner类
├── dsl_constructor.py    # DSL构造辅助工具
├── test_refiner.py       # 测试文件
└── zero_shot_refine.py   # 原有的zero-shot实现（参考）
```
