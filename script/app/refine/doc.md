# DSL Refine
## 文档目标
定义一个基于DSL+人工提供FP的refine流程。
改流程以LLM为核心推理组件，以规则化文本处理为执行组件，通过从FP中或者原始代码对中没有注意到的程序元素中提取额外的DSL约束，增强DSL约束从而消除误报。
功能代码位置：app/refine目录下
实验代码位置：thesis/refine目录下（先不修改，在功能代码位置先写简单测试）

## 输入数据
### DSL
1. dsl_code # 用于检测某种缺陷的DSL代码
2. buggy_code
3. fixed_code # 一对缺陷样例，DSL代码从这对代码中提取出来的
4. root_cause # 缺陷样例的缺陷原因

### FP记录
1. fp代码 # 用该DSL检测出来的误报代码

## 中间数据（LLM输出解析）
需要设计
1. 额外约束（从原始缺陷样例中提取或者从FP提取），包含约束位置（DSL节点）以及具体约束信息

## 输出数据
1. 添加额外约束的DSL代码

## Refine流程
1. 分析DSL数据
2. 分析FP原因
3. 提取额外约束
4. 构造最终DSL

## DSL语法
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

## 详细流程

### LLM接口
参考interface/llm/llm_openai.py和llm_pool.py

### 设计模式
参考状态机模式，例如app/abs/classified_topdown/prompt_state.py以及inference.py
但原始设计过于动态，我希望参考类似interface的设计

### Step0 
1. 设计LLM_context数据结构，方便上下文管理
2. 设计对应状态，状态迁移规则

### Step1 分析DSL数据
1. 设计Prompt，记录在prompt文件中，需要让LLM理解任务并获得输入


### Step2 分析FP原因
1. 设计Prompt，让LLM解释为什么当前DSL匹配该FP，建立DSL具体节点和FP代码元素的关系

### Step3 提取额外约束
1. 状态分支，根据LLM分析结果判定不满足的约束出现在哪个文件中（buggy,fixed,fp）
2. 新状态，提供对应文件进行额外约束的提取，需要满足额外约束的数据结构
3. 存储模型交互上下文，以及提取的约束

### Step4 构造最终DSL
1. 将额外约束转化DSL的条件子句
2. 根据额外约束的节点位置拼接DSL
3. 存储refine后的结果

### test
1. dsl_code输入：E:/dataset/Navi/final_thesis_datas/ori_dsl/pmd_v1_commits/AvoidInstanceofChecksInCatchClause/3/2.kirin
2. buggy_code: E:/dataset/Navi/DEFs/pmd/AvoidInstanceofChecksInCatchClause/3/2/buggy.java
3. fixed_code: E:/dataset/Navi/DEFs/pmd/AvoidInstanceofChecksInCatchClause/3/2/fixed.java
4. root_cause: E:/dataset/Navi/DEFs/pmd/AvoidInstanceofChecksInCatchClause/3/2/info.json load进来dict的'may_be_fixed_violations'字段
5. fp代码：E:/dataset/Navi/final_thesis_datas/ori_dsl_detect_results/pmd_v1_commits/AvoidInstanceofChecksInCatchClause/3/2_1_labeled_results.json 的list[dict]用label="fp"进行过滤的第一个dict的'method_source'字段
