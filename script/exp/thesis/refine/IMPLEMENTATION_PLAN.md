# DSL迭代优化实验设计 (Iterative DSL Refinement Experiment Design)

## 1. 实验目标 (Objective)

通过迭代式的FP反馈，逐步优化DSL规则，研究约束精炼对检测结果的影响。

**核心研究问题：**
- RQ1: 原始DSL在进行缺陷检测方面表现怎么样
- RQ2: 使用一个FP进行约束优化，能否减少其他FP的数量？（约束泛化能力）
- RQ3: 迭代优化的收敛速度如何？需要多少轮迭代才能稳定？
- RQ4: 不同Checker的优化效果差异？哪些类型的规则更容易优化？
- RQ5: （Optional）优化后的DSL质量如何？（humancheck）

---

## 2. 实验数据组织 (Data Organization)

### 2.1 输入数据结构

**原始数据集根目录：** `E:/dataset/Navi/`

```
E:/dataset/Navi/
├── final_thesis_datas/
│   ├── ori_dsl/                          # 原始DSL文件
│   │   ├── pmd_v1_commits/
│   │   │   ├── {checker_name}/
│   │   │   │   ├── {group}/
│   │   │   │   │   └── {case}.kirin
│   │   └── codeql_v1_commits/...
│   │
│   └── ori_dsl_detect_results/           # 原始检测结果（Iteration 0的FP来源）
│       ├── pmd_v1_commits/
│       │   ├── {checker_name}/
│       │   │   ├── {group}/
│       │   │   │   └── {dsl_case}_{scanned_case}_labeled_results.json
│       └── ...
│
├── DEFs/                                 # 缺陷修复数据
│   ├── pmd/
│   │   ├── {checker_name}/
│   │   │   ├── {group}/
│   │   │   │   ├── {case}/
│   │   │   │   │   ├── buggy.java
│   │   │   │   │   ├── fixed.java
│   │   │   │   │   └── info.json
│   └── codeql/...
│
└── rq2_commit/                           # commit数据（用于检测）
    ├── pmd_v1_commits/
    │   ├── {checker_name}/
    │   │   ├── {group}/
    │   │   │   ├── {scanned_case}/
    │   │   │   │   ├── after/            # 检测目标代码库
    │   │   │   │   ├── methods.json
    │   │   │   │   └── sat_warnings.json
    └── ...
```

### 2.2 输出数据结构

**实验输出根目录：** `E:/dataset/Navi/final_thesis_datas/iterExp/`

```
E:/dataset/Navi/final_thesis_datas/iterExp/
├── iteration_0/                          # 第0轮（原始DSL，直接复制）
│   ├── pmd_v1_commits/
│   │   ├── {checker_name}/
│   │   │   ├── {group}/
│   │   │   │   ├── {case}/
│   │   │   │   │   ├── dsl.kirin                      # 本轮使用的DSL（iteration 0直接复制原始DSL）
│   │   │   │   │   ├── task_info.json                 # 整合所有信息（FP选择、指标、状态等）
│   │   │   │   │   ├── detect_results/                # 检测结果目录
│   │   │   │   │   │   └── {dsl_case}_{scanned_case}_labeled_results.json
│   │   │   │   │   └── refine_context.json            # refine上下文（仅iteration>=1有）
│   └── ...
│
├── iteration_1/                          # 第1轮（基于iteration_0的FP进行refine）
│   ├── pmd_v1_commits/
│   │   ├── {checker_name}/
│   │   │   ├── {group}/
│   │   │   │   ├── {case}/
│   │   │   │   │   ├── dsl.kirin                      # refine后的DSL
│   │   │   │   │   ├── task_info.json                 # 整合所有信息
│   │   │   │   │   ├── detect_results/                # 检测结果目录
│   │   │   │   │   │   └── {dsl_case}_{scanned_case}_labeled_results.json
│   │   │   │   │   └── refine_context.json            # refine上下文
│   └── ...
│
├── iteration_N/
│   └── ...
│
└── iteration_summary.json                # 每轮迭代的汇总报告
```

---

## 3. 实验流程设计 (Workflow Design)

### 3.1 任务标识 (Task ID)

每个DSL任务使用相对路径作为唯一标识：
- 格式：`{dataset}/{checker_name}/{group}/{case}`
- 示例：`pmd_v1_commits/AvoidInstanceofChecksInCatchClause/3/2`

### 3.2 迭代流程 (Iteration Flow)

```
┌─────────────────────────────────────────────────────────────────┐
│ Iteration 0: 初始化阶段                                          │
├─────────────────────────────────────────────────────────────────┤
│ 1. 扫描所有原始DSL文件                                           │
│    输入：E:/dataset/Navi/final_thesis_datas/ori_dsl/**/*.kirin │
│                                                                 │
│ 2. 对每个DSL任务：                                              │
│    a. 复制原始DSL到 iteration_0/{task_id}/dsl.kirin            │
│    b. 从 ori_dsl_detect_results 读取原始检测结果               │
│    c. 统计FP数量：                                              │
│       - 如果 FP == 0: 标记停止 (原因: no_fp)                   │
│       - 如果 FP >= 1: 记录FP总数（不选择FP）                   │
│    d. 保存task_info.json（包含FP统计、detection_metrics等）    │
│                                                                 │
│ 3. 输出：iteration_0/ 目录（包含所有任务的初始状态）            │
└─────────────────────────────────────────────────────────────────┘

                            ↓ 用户手动操作：无需检测，iteration_0直接使用原始检测结果

┌─────────────────────────────────────────────────────────────────┐
│ Iteration N (N >= 1): 优化迭代阶段                              │
├─────────────────────────────────────────────────────────────────┤
│ 1. 加载上一轮状态并选择FP                                       │
│    a. 读取 iteration_{N-1}/ 的所有任务和检测结果               │
│    b. 过滤已停止的任务                                          │
│    c. 对每个活跃任务：                                          │
│       - 从 iteration_{N-1}/detect_results/*.json 读取FP列表    │
│       - 根据 fp_history 过滤已使用的FP                         │
│       - 如果剩余FP == 0: 标记停止 (all_fps_exhausted)          │
│       - 否则：选择1个未使用的FP，保存到 fp_selection            │
│                                                                 │
│ 2. 对每个活跃任务执行refine：                                   │
│    a. 构建 RefineInput:                                         │
│       - dsl_code: iteration_{N-1}/{task_id}/dsl.kirin          │
│       - buggy/fixed/root_cause: DEFs/{tool}/{checker}/{group}/{case}/ │
│       - fp_code: 从本轮的 fp_selection.selected_fp 提取        │
│    b. 调用 DSLRefiner.refine()                                 │
│    c. 保存结果到 iteration_N/{task_id}/:                       │
│       - dsl.kirin (refine后的DSL)                              │
│       - refine_context.json                                    │
│       - task_info.json (包含本轮FP选择和refine metrics)        │
│                                                                 │
│ 3. 暂停，等待用户手动检测                                       │
└─────────────────────────────────────────────────────────────────┘

                            ↓ 用户手动操作：运行检测工具

┌─────────────────────────────────────────────────────────────────┐
│ 用户手动检测步骤                                                 │
├─────────────────────────────────────────────────────────────────┤
│ 1. 使用框架提供的检测脚本进行批量检测：                          │
│    python exp/thesis/refine/run_detection.py --iteration N     │
│    (读取 iteration_N/下所有任务的dsl.kirin进行检测)            │
│                                                                 │
│ 2. 使用框架提供的标注脚本进行批量标注：                          │
│    python exp/thesis/refine/run_labeling.py --iteration N      │
│    (收集检测结果并标注TP/FP/FN，计算Precision/Recall/F1)       │
│    (结果会自动更新到 task_info.json 的 metrics.detection_metrics) │
│                                                                 │
│ 3. 结果自动保存到：                                              │
│    iteration_N/{dataset}/{checker}/{group}/{case}/detect_results/{dsl_case}_{scanned_case}_labeled_results.json │
└─────────────────────────────────────────────────────────────────┘

                            ↓ 用户确认检测完成，继续下一轮

                            回到 "Iteration N" 步骤
                            （下一轮会重新选择FP并执行refine）
```

---

## 4. 核心设计决策 (Design Decisions)

### 4.1 Iteration 0 的定义 ✅

- **Iteration 0 = 原始DSL的直接复制**
- 不执行任何refine操作
- 直接使用 `ori_dsl_detect_results/` 中的原始检测结果
- 目的：建立baseline，便于后续对比

### 4.2 FP选择策略 ✅

**选择算法：每轮只选择1个FP进行优化**
```
1. 从检测结果中提取所有 label == "fp" 的条目
2. 过滤掉历史已使用的FP（根据所有轮次的fp_history）
3. 如果剩余FP数量 == 0:
   → 停止迭代（原因：all_fps_exhausted）
4. 如果剩余FP数量 >= 1:
   → 随机选择1个FP（或按顺序选择第一个未使用的FP）
   → 保存该FP的完整信息（不只是hash）
```

**task_info.json 格式（整合所有信息）：**

**注意区分Iteration 0和Iteration N：**
- **Iteration 0**: 只有`detection_metrics`，无`fp_selection`和`fp_history`（未开始refine）
- **Iteration N (N >= 1)**: 包含`fp_selection`（本轮选择）、`fp_history`（历史选择）和`metrics`（refine和detection指标）

**Iteration 0的task_info.json示例：**
```json
{
  "task_id": "pmd_v1_commits/AvoidInstanceofChecksInCatchClause/3/2",
  "iteration": 0,
  "status": "active",

  "metrics": {
    "detection_metrics": {
      "total_warnings": 30,
      "tp_count": 20,
      "fp_count": 10,
      "fn_count": 3,
      "precision": 0.67,
      "recall": 0.87,
      "f1_score": 0.76
    }
  },

  "timestamp": "2024-01-21 09:00:00"
}
```

**Iteration N (N >= 1)的task_info.json示例：**
```json
{
  "task_id": "pmd_v1_commits/AvoidInstanceofChecksInCatchClause/3/2",
  "iteration": 2,
  "status": "active",

  "fp_selection": {
    "total_fps_in_results": 8,
    "remaining_fps_count": 7,
    "selected_fp": {
      "label": "fp",
      "file_name": "src/main/java/Foo.java",
      "function_name": "bar",
      "begin_line": 42,
      "end_line": 55,
      "report_line": 45,
      "method_signature": "public void bar(String arg)",
      "code_snippet": "public void bar(String arg) {\n  // ...\n}"
    }
  },

  "fp_history": {
    "iteration_1": {
      "label": "fp",
      "file_name": "src/main/java/Baz.java",
      "function_name": "qux",
      "begin_line": 10,
      "end_line": 20,
      "report_line": 15,
      "method_signature": "public int qux()",
      "code_snippet": "public int qux() {\n  // ...\n}"
    },
    "iteration_2": {
      "label": "fp",
      "file_name": "src/main/java/Foo.java",
      "function_name": "bar",
      "begin_line": 42,
      "end_line": 55,
      "report_line": 45,
      "method_signature": "public void bar(String arg)",
      "code_snippet": "public void bar(String arg) {\n  // ...\n}"
    }
  },

  "metrics": {
    "refine_success": true,
    "refine_time_seconds": 45.2,
    "prompt_tokens": 5234,
    "completion_tokens": 892,
    "total_tokens": 6126,

    "detection_metrics": {
      "total_warnings": 25,
      "tp_count": 18,
      "fp_count": 7,
      "fn_count": 2,
      "precision": 0.72,
      "recall": 0.90,
      "f1_score": 0.80
    }
  },

  "timestamp": "2024-01-21 10:00:00"
}
```

### 4.3 停止条件 ✅

| 停止原因 | 触发条件 | 记录在 task_info.json |
|---------|---------|---------------------|
| `no_fp` | Iteration 0时原始检测结果无FP | `"status": "stopped", "stop_reason": "no_fp", "stopped_at": 0` |
| `no_fp_converged` | Iteration N检测后FP数量降为0 | `"status": "stopped", "stop_reason": "no_fp_converged", "stopped_at": N` |
| `all_fps_exhausted` | 所有FP都已使用过 | `"status": "stopped", "stop_reason": "all_fps_exhausted", "stopped_at": N` |
| `refine_failed` | DSLRefiner执行失败 | `"status": "stopped", "stop_reason": "refine_failed", "stopped_at": N, "error": "..."` |

**注意：** 不设置最大迭代次数，每轮完成后框架停止，由用户手动控制下一轮迭代

### 4.4 检测结果处理 ✅

**检测工具集成：** 框架内集成
- 框架提供 `run_detection.py` 脚本（参考 `rerun_rq2_dsl_detect.py` 实现）
- 框架提供 `run_labeling.py` 脚本（参考 `collect_pre_merge_functions.py` 实现）
- 每轮refine完成后，框架**停止**并提示用户运行检测/标注脚本
- 用户运行检测和标注脚本后，使用 `--iteration N+1` 参数启动下一轮refine

**检测脚本位置：**
- [exp/thesis/refine/run_detection.py](exp/thesis/refine/run_detection.py) - 批量检测脚本
- [exp/thesis/refine/run_labeling.py](exp/thesis/refine/run_labeling.py) - 批量标注脚本

**检测结果文件命名：**
- 格式：`{dsl_case}_{scanned_case}_labeled_results.json`
- 示例：`2_1_labeled_results.json`（DSL case=2, scanned case=1）
- 保存位置：`iteration_N/{dataset}/{checker}/{group}/{case}/detect_results/`

### 4.5 LLM池调度 ✅

- 复用现有的 `AsyncLLMPool` (interface/llm/llm_pool.py)
- **绑定策略：** 一个DSL任务独占一个LLM实例（整个生命周期）
- **并发控制：** FIFO队列，超出LLM池大小的任务进入等待队列
- **原因：** 避免上下文混淆，保证同一任务的所有LLM调用使用相同的client

### 4.6 路径映射规则 ✅

**相对路径结构：** `{dataset}/{checker_name}/{group}/{case}`

**映射关系：**
```python
# 示例任务ID: pmd_v1_commits/AvoidInstanceofChecksInCatchClause/3/2

# 原始DSL
dsl_path = ori_dsl_root / task_id + ".kirin"
# → E:/dataset/Navi/final_thesis_datas/ori_dsl/pmd_v1_commits/AvoidInstanceofChecksInCatchClause/3/2.kirin

# 原始检测结果（Iteration 0）
detect_result_path = ori_dsl_detect_results_root / dataset / checker / group / f"{dsl_case}_{scanned_case}_labeled_results.json"
# → E:/dataset/Navi/final_thesis_datas/ori_dsl_detect_results/pmd_v1_commits/AvoidInstanceofChecksInCatchClause/3/2_1_labeled_results.json

# DEF数据
buggy_path = defs_root / tool / checker / group / case / "buggy.java"
# → E:/dataset/Navi/DEFs/pmd/AvoidInstanceofChecksInCatchClause/3/2/buggy.java

# Iteration N的输出
iteration_output_dir = iter_exp_root / f"iteration_{N}" / task_id
# → E:/dataset/Navi/final_thesis_datas/iterExp/iteration_1/pmd_v1_commits/AvoidInstanceofChecksInCatchClause/3/2/

# Iteration N的检测结果（用户手动放置）
iteration_detect_result = iteration_output_dir / "detect_results" / f"{dsl_case}_{scanned_case}_labeled_results.json"
# → E:/dataset/Navi/final_thesis_datas/iterExp/iteration_1/.../detect_results/2_1_labeled_results.json
```

---

## 5. 实验配置 (Configuration)

### 5.1 路径配置

```python
# 基础路径
DATASET_BASE = Path("E:/dataset/Navi")

# 输入路径
ORI_DSL_ROOT = DATASET_BASE / "final_thesis_datas/ori_dsl"
ORI_DETECT_RESULTS_ROOT = DATASET_BASE / "final_thesis_datas/ori_dsl_detect_results"
DEFS_ROOT = DATASET_BASE / "DEFs"
COMMIT_ROOT = DATASET_BASE / "rq2_commit"

# 输出路径
ITER_EXP_ROOT = DATASET_BASE / "final_thesis_datas/iterExp"
```

### 5.2 实验参数

```python
# FP选择配置
FP_SELECTION_STRATEGY = "sequential"  # 每轮选择1个FP（sequential: 顺序选择，random: 随机选择）

# LLM配置
LLM_POOL_SIZE = 5               # LLM池大小
TASK_TIMEOUT = 300              # 单任务超时（秒）

# 实验范围（支持glob模式）
DSL_PATTERNS = [
    "pmd_v1_commits/**/*.kirin",
    "codeql_v1_commits/**/*.kirin"
]

# 注意：不设置MAX_ITERATIONS，每轮完成后由用户手动启动下一轮
```

---

## 6. 实验指标 (Metrics)

### 6.1 基础指标

**每个任务的metrics字段（在task_info.json中）:**
```json
{
  "metrics": {
    "refine_success": true,
    "refine_time_seconds": 45.2,
    "prompt_tokens": 5234,
    "completion_tokens": 892,
    "total_tokens": 6126,

    "detection_metrics": {
      "total_warnings": 25,
      "tp_count": 15,
      "fp_count": 10,
      "fn_count": 2,
      "precision": 0.60,
      "recall": 0.88,
      "f1_score": 0.71
    }
  }
}
```

**每轮汇总 (iteration_summary.json):**
```json
{
  "iteration": 1,
  "total_tasks": 100,
  "active_tasks": 85,
  "stopped_tasks": 15,
  "refine_success_count": 82,
  "refine_failed_count": 3,
  "total_time_seconds": 1234.5,
  "total_tokens": 520000,
  "stop_reasons": {
    "no_fp": 10,
    "refine_failed": 3,
    "all_fps_exhausted": 2
  }
}
```

### 6.2 效果评估指标（后续分析）

**针对RQ的评估指标：**

- **RQ1 (原始DSL检测表现):**
  - Iteration 0的Precision/Recall/F1分布
  - 各Checker的初始检测质量对比
  - TP/FP/FN统计分析

- **RQ2 (约束泛化能力):**
  - 每轮使用1个FP优化后，其他FP的消除数量
  - FP消除率曲线（iteration vs remaining_fps）
  - 单次优化的FP影响范围分析

- **RQ3 (收敛速度):**
  - 平均迭代轮次（达到no_fp_converged或all_fps_exhausted）
  - 收敛曲线（FP数量随迭代变化）
  - 停止原因分布统计

- **RQ4 (不同Checker差异):**
  - 各Checker的平均收敛轮次
  - 各Checker的FP消除率对比
  - 各Checker的Precision/Recall/F1变化趋势

- **RQ5 (可选：人工检查):**
  - 生成约束的语法/语义错误率
  - 需要人工干预的案例统计
  - LLM生成约束的质量评分

---

## 7. 实验执行步骤 (Execution Steps)

### 7.1 准备阶段

```bash
# 1. 配置实验参数
vim exp/thesis/refine/experiment_config.yaml

# 2. 验证数据完整性
python exp/thesis/refine/validate_dataset.py
```

### 7.2 迭代执行阶段

**完整的单轮迭代流程：**

```bash
# ===== Iteration 0: 初始化 =====
python exp/thesis/refine/batch_runner.py --iteration 0
# → 复制原始DSL，选择FP，保存task_info.json
# → 框架停止，提示用户进行检测（但iteration 0使用原始检测结果，无需重新检测）

# ===== Iteration 1: 第一次refine =====
python exp/thesis/refine/batch_runner.py --iteration 1
# → 读取iteration_0的task_info.json和FP选择
# → 执行refine，保存refined DSL和task_info.json
# → 框架停止，提示用户运行检测脚本

# 用户手动运行检测和标注
python exp/thesis/refine/run_detection.py --iteration 1
python exp/thesis/refine/run_labeling.py --iteration 1
# → 结果保存到 iteration_1/.../detect_results/

# ===== Iteration 2: 第二次refine =====
python exp/thesis/refine/batch_runner.py --iteration 2
# → 读取iteration_1的检测结果
# → 更新task_info.json，选择新的FP
# → 执行refine，保存refined DSL
# → 框架停止，提示用户运行检测脚本

# ... 重复直到用户手动停止或所有任务都标记为stopped
```

### 7.3 分析阶段

```bash
# 查看当前实验状态
python exp/thesis/refine/batch_runner.py --status

# 生成实验总结报告
python exp/thesis/refine/analyze_results.py
```
