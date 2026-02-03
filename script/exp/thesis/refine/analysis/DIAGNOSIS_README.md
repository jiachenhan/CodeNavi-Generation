# DSL迭代优化 - 问题任务诊断工具

## 功能说明

该诊断工具用于分析在DSL迭代优化过程中效果变差的任务,帮助定位优化策略的问题。

## 诊断维度

### 1. 语法错误任务 (Syntax Error)
- **定义**: Refine过程失败,导致生成的DSL存在语法错误
- **严重级别**: Critical
- **可能原因**:
  - LLM生成的constraints存在语法错误
  - Constraints与现有DSL结构冲突
  - 提示词设计问题

### 2. 召回率下降任务 (Recall Drop)
- **定义**: 召回率下降超过阈值(默认5%)
- **严重级别**: High(下降>10%) / Medium(下降5-10%)
- **可能原因**:
  - 新增constraints过于严格,过滤了本应检测的TP
  - Constraints定义不准确,误杀了正常bug
  - FP选择策略有问题,选中的FP不具代表性

### 3. 精确率下降任务 (Precision Drop)
- **定义**: 精确率下降超过阈值(默认5%)
- **严重级别**: High(下降>10%) / Medium(下降5-10%)
- **可能原因**:
  - 新增constraints未能有效过滤FP
  - Constraints定义过于宽松
  - FP选择的case特异性太强,无法泛化

### 4. 完全无改进任务 (No Improvement)
- **定义**: F1分数未提升且FP数量未减少
- **严重级别**: Medium
- **可能原因**:
  - 选中的FP已经是edge case,难以通过constraints过滤
  - 所有可用FP已耗尽
  - 优化策略不适合该任务

---

## 使用方法

### 基本用法

```bash
# 长格式
python -m exp.thesis.refine.analysis.diagnose_bad_cases \
  --initial-iteration 0 \
  --final-iteration 1 \
  --output-dir ./diagnosis_reports

# 短格式（推荐）
python -m exp.thesis.refine.analysis.diagnose_bad_cases -i 0 -f 1 -o ./diagnosis_reports

# 使用默认输出目录
python -m exp.thesis.refine.analysis.diagnose_bad_cases -i 0 -f 1
```

### 参数说明

#### 必需参数

- `-i, --initial-iteration` (int, 必需)
  - 初始迭代轮次,通常是0
  - 示例: `-i 0`

- `-f, --final-iteration` (int, 必需)
  - 最终迭代轮次
  - 示例: `-f 1`

#### 可选参数

- `-o, --output-dir` (str, 可选)
  - 输出目录路径
  - 示例: `-o ./diagnosis_reports`
  - 默认: `./exp/thesis/refine/analysis/out/diagnosis`

- `-r, --recall-drop-threshold` (float, 默认: 0.05)
  - 召回率下降阈值(5% = 0.05)
  - 示例: `-r 0.1` (设为10%)

- `-p, --precision-drop-threshold` (float, 默认: 0.05)
  - 精确率下降阈值(5% = 0.05)
  - 示例: `-p 0.1` (设为10%)

---

## 输出文件

### 1. diagnosis_report.json
诊断数据报告(JSON格式),包含:

```json
{
  "total_tasks": 223,
  "iterations_analyzed": [0, 1],
  "syntax_error_tasks": [...],
  "recall_drop_tasks": [...],
  "precision_drop_tasks": [...],
  "no_improvement_tasks": [...],
  "summary": {
    "total_problem_tasks": 50,
    "problem_rate": 0.2242,
    "problem_counts": {
      "syntax_error": 10,
      "recall_drop": 20,
      "precision_drop": 15,
      "no_improvement": 5
    },
    "problem_tool_breakdown": {...},
    "avg_metrics": {
      "avg_recall_drop": -0.0823,
      "avg_precision_drop": -0.0612
    }
  }
}
```

**单个问题任务的详细信息**:
```json
{
  "task_id": "pmd_v1_commits/AvoidInstanceof/3/1",
  "tool": "pmd",
  "status": "stopped",
  "stop_reason": "refine_failed",
  "precision_delta": -0.0234,
  "recall_delta": -0.0823,
  "f1_delta": -0.0512,
  "fp_delta": 2,
  "initial_precision": 0.6500,
  "initial_recall": 0.7200,
  "initial_f1": 0.6800,
  "initial_fp_count": 8,
  "final_precision": 0.6266,
  "final_recall": 0.6377,
  "final_f1": 0.6288,
  "final_fp_count": 10,
  "refine_success": false,
  "refine_error": "SyntaxError: Expected ']' at line 45",
  "problem_type": "syntax_error",
  "severity": "critical"
}
```

### 2. diagnosis_overview.png
诊断概览图表(4个子图):

- **子图1**: 问题类型分布(饼图) - 展示各类问题的占比
- **子图2**: 问题任务按工具分布(堆叠柱状图) - PMD vs CodeQL的问题分布
- **子图3**: 召回率下降分析(散点图) - 初始召回率 vs 召回率变化,气泡大小表示FP变化
- **子图4**: 精确率下降分析(散点图) - 初始精确率 vs 精确率变化,气泡大小表示FP变化

### 3. top_bad_cases.png
Top 20效果最差任务对比图(水平柱状图):

- 按F1变化排序,显示效果下降最严重的20个任务
- 颜色表示严重级别(Critical/High/Medium/Normal)
- 标签显示任务ID和问题类型

---

## 使用示例

### 示例1: 诊断iteration_0到iteration_1的任务

```bash
# 使用短格式
python -m exp.thesis.refine.analysis.diagnose_bad_cases -i 0 -f 1

# 或指定输出目录
python -m exp.thesis.refine.analysis.diagnose_bad_cases -i 0 -f 1 -o ./diagnosis_reports
```

**输出**:
```
DSL迭代优化 - 效果差的任务诊断工具
================================================================================
Initial iteration: 0
Final iteration: 1
Recall drop threshold: 0.05
Precision drop threshold: 0.05
Output directory: diagnosis_reports
Initializing components...
Diagnosing tasks...
JSON report saved to: diagnosis_reports/diagnosis_report.json

生成可视化图表...
Chart saved to diagnosis_reports/diagnosis_overview.png
Chart saved to diagnosis_reports/top_bad_cases.png

可视化图表已保存:
  - 诊断概览: diagnosis_reports/diagnosis_overview.png
  - Top 20最差任务: diagnosis_reports/top_bad_cases.png

================================================================================
诊断完成!
================================================================================
总任务数: 223
问题任务数: 50 (22.42%)

问题分类:
  - 语法错误(refine失败): 10
  - 召回率下降: 20
  - 精确率下降: 15
  - 完全无改进: 5

平均指标变化(仅问题任务):
  - 平均召回率下降: -0.0823
  - 平均精确率下降: -0.0612

输出文件: diagnosis_reports/diagnosis_report.json

================================================================================
Top 10 最严重的问题任务:
================================================================================

1. [CRITICAL] pmd_v1_commits/AvoidInstanceof/3/1
   问题类型: syntax_error
   工具: PMD
   F1变化: -0.0512 (Recall: -0.0823, Precision: -0.0234)
   FP变化: +2 (8 → 10)
   Refine错误: SyntaxError: Expected ']' at line 45...

2. [HIGH] codeql_v1_commits/DeadCode/2/1
   问题类型: recall_drop
   工具: CODEQL
   F1变化: -0.1234 (Recall: -0.1500, Precision: +0.0123)
   FP变化: -3 (10 → 7)

...
```

### 示例2: 自定义阈值(更严格)

```bash
# 使用短格式，设置更严格的阈值（10%）
python -m exp.thesis.refine.analysis.diagnose_bad_cases -i 0 -f 5 -r 0.1 -p 0.1
```

这将只标记召回率/精确率下降超过10%的任务为问题。

---

## 如何利用诊断结果优化策略

### 1. 如果大量语法错误任务
**问题**: LLM生成的constraints存在语法问题

**优化方向**:
- 改进constraints生成的提示词,强调语法正确性
- 增加语法验证步骤,让LLM自我修正
- 提供更多DSL语法示例
- 使用更强的模型(例如从Sonnet升级到Opus)

### 2. 如果召回率普遍下降
**问题**: 新增的constraints过于严格

**优化方向**:
- 调整FP选择策略,避免选择过于特殊的case
- 改进constraints生成提示词,强调"只过滤FP,不误杀TP"
- 分析Top召回率下降任务,检查它们的constraints是否过度泛化
- 考虑引入constraints验证机制,评估其对TP的影响

### 3. 如果精确率普遍下降
**问题**: Constraints未能有效过滤FP

**优化方向**:
- 检查选中的FP是否具有共性特征
- 改进FP特征提取逻辑
- 调整constraints生成策略,确保覆盖FP的核心特征
- 考虑多轮refine,逐步优化

### 4. 如果完全无改进任务较多
**问题**: 优化策略遇到瓶颈

**优化方向**:
- 分析这些任务是否已达到DSL表达能力上限
- 考虑使用更复杂的约束类型
- 检查是否所有可用FP已耗尽
- 考虑调整停止条件

---

## 与全局分析工具的区别

| 工具 | 用途 | 输出 |
|------|------|------|
| **run_analysis.py** | 全局演化分析 | 整体指标变化趋势,适合评估整体效果 |
| **diagnose_bad_cases.py** | 问题任务诊断 | 识别和分析效果变差的任务,适合优化策略 |

**推荐工作流**:
1. 先运行 `run_analysis.py` 查看整体效果
2. 如果发现指标下降或改进不明显,运行 `diagnose_bad_cases.py` 定位问题
3. 根据诊断结果调整优化策略
4. 重新运行实验
5. 重复上述步骤

---

## 常见问题

### Q1: 为什么total_tasks和问题任务数之和不等于active_tasks?
A: 因为有些任务在iteration_0时就停止了(例如NO_FP),这些任务不会被纳入问题统计(因为它们没有经过refine过程)。

### Q2: 如何确定合适的阈值?
A: 默认阈值(5%)是根据经验设定的。如果你的数据集整体指标波动较大,可以适当提高阈值(例如10%)。

### Q3: 诊断报告中的"count"为什么减少了?
A: 这是因为在fallback统计时,stopped任务的tool_stats可能未被正确统计。这是已知问题,将在后续版本修复。

### Q4: 如何导出问题任务列表进行人工检查?
A: 可以从 `diagnosis_report.json` 中提取 `syntax_error_tasks`, `recall_drop_tasks` 等列表,它们包含了每个问题任务的task_id和详细信息。

---

## 后续增强建议

1. **自动化根因分析** - 基于refine_context自动分析constraints的问题
2. **Constraints质量评估** - 评估生成的constraints的合理性
3. **对比不同优化策略** - 支持A/B测试不同的FP选择或prompt策略
4. **任务聚类分析** - 自动识别具有相似问题的任务群组
