# DSL迭代优化实验框架

基于FP反馈的迭代式DSL优化实验框架，用于验证RQ2和RQ3。

## 快速开始

### 1. 配置实验参数

实验配置在 [experiment_config.py](experiment_config.py) 中定义：

```python
# 主要配置项
DATASET_BASE = "E:/dataset/Navi"
LLM_POOL_SIZE = 5
FP_SELECTION_STRATEGY = FPSelectionStrategy.SEQUENTIAL  # 或 RANDOM
```

### 2. 运行Iteration 0（初始化）

```bash
cd script
python exp/thesis/refine/batch_runner.py --iteration 0
```

**Iteration 0做什么？**
- 扫描所有DSL文件
- 复制原始DSL到 `iteration_0/` 目录
- 读取原始检测结果（来自 `ori_dsl_detect_results/`）
- 计算初始的detection_metrics（TP/FP/FN）
- 如果FP=0，标记任务为stopped

**输出目录结构：**
```
E:/dataset/Navi/final_thesis_datas/iterExp/
└── iteration_0/
    ├── pmd_v1_commits/
    │   └── {checker}/{group}/{case}/
    │       ├── dsl.kirin              # 原始DSL（复制）
    │       └── task_info.json         # 任务信息和metrics
    └── iteration_summary.json         # 本轮汇总
```

### 3. 运行Iteration 1（第一次refine）

```bash
python exp/thesis/refine/batch_runner.py --iteration 1
```

**Iteration 1做什么？**
- 从Iteration 0的检测结果中选择1个FP
- 使用选中的FP进行约束优化（调用DSLRefiner）
- 保存refined DSL和refine_context
- 更新task_info.json（包含fp_selection和fp_history）

**输出目录结构：**
```
iteration_1/
└── {task_id}/
    ├── dsl.kirin                      # refine后的DSL
    ├── task_info.json                 # 包含fp_selection和refine_metrics
    └── refine_context.json            # 完整上下文（用于调试）
```

### 4. 运行检测脚本

```bash
# 对iteration 1的refined DSL进行检测
python exp/thesis/refine/run_detection.py --iteration 1
```

**检测脚本做什么？**
- 读取 `iteration_1/` 下的所有refined DSL
- 对每个DSL运行Kirin引擎检测
- 保存检测结果到 `iteration_1/{task_id}/detect_results/`

**输出：**
```
iteration_1/
└── {task_id}/
    └── detect_results/
        └── 1_2/                       # case1的DSL检测case2
            └── *.xml                  # Kirin引擎输出
```

### 5. 运行标注脚本

```bash
# 标注检测结果并更新metrics
python exp/thesis/refine/run_labeling.py --iteration 1
```

**标注脚本做什么？**
- 解析Kirin引擎的XML输出
- 与ground truth（sat_warnings.json）对比，标注TP/FP/FN
- 保存标注结果到 `*_labeled_results.json`
- 更新task_info.json的detection_metrics

**输出：**
```
iteration_1/
└── {task_id}/
    ├── detect_results/
    │   └── 1_2_labeled_results.json   # 标注后的结果
    └── task_info.json                 # 更新了detection_metrics
```

### 6. 运行Iteration 2（继续迭代）

```bash
python exp/thesis/refine/batch_runner.py --iteration 2
```

**重复步骤3-6，直到：**
- 所有任务都标记为stopped（no_fp_converged或all_fps_exhausted）
- 或达到您期望的迭代轮次

## 完整工作流程

```
Iteration 0:
  batch_runner.py --iteration 0
    ↓
  (使用原始检测结果，无需运行检测)
    ↓
Iteration 1:
  batch_runner.py --iteration 1
    ↓
  run_detection.py --iteration 1
    ↓
  run_labeling.py --iteration 1
    ↓
Iteration 2:
  batch_runner.py --iteration 2
    ↓
  run_detection.py --iteration 2
    ↓
  run_labeling.py --iteration 2
    ↓
  ... (重复直到所有任务停止)
```

## 停止条件

任务会在以下情况下停止迭代：

| 停止原因 | 触发条件 | 说明 |
|---------|---------|------|
| `no_fp` | Iteration 0时检测结果无FP | 原始DSL已经很好，无需优化 |
| `no_fp_converged` | Iteration N后FP数量降为0 | 优化成功，FP全部消除 |
| `all_fps_exhausted` | 所有FP都已使用过 | 已尝试所有FP，无法继续优化 |
| `refine_failed` | DSLRefiner执行失败 | 技术错误，需要人工检查 |

## 重要参数说明

### FP选择策略

```python
# experiment_config.py
fp_selection_strategy = FPSelectionStrategy.SEQUENTIAL  # 顺序选择第一个未使用的FP
# 或
fp_selection_strategy = FPSelectionStrategy.RANDOM      # 随机选择未使用的FP
```

### LLM池大小

```python
llm_pool_size = 5  # 并发处理5个任务
```

### 限制任务数量（用于测试）

```bash
python batch_runner.py --iteration 0 --limit 10  # 只处理前10个任务
```

### scanned_case自动推断机制 ⭐

**框架会自动从检测结果文件名中提取scanned_case**，无需手动指定。

**推断逻辑：**
- **Iteration 0**: 从 `ori_dsl_detect_results/{task_id}/{dsl_case}_{scanned_case}_labeled_results.json` 提取
  - 例如: `1_2_labeled_results.json` → scanned_case=2
- **Iteration N (N>=1)**: 从上一轮的 `iteration_{N-1}/{task_id}/detect_results/{dsl_case}_{scanned_case}_labeled_results.json` 提取
- **找不到时**: 使用默认值 scanned_case=2

**正常使用（自动推断）：**
```bash
# 无需指定 --scanned-case，框架自动推断
python batch_runner.py --iteration 0
python run_detection.py --iteration 1
python run_labeling.py --iteration 1
```

**RQ4验证（手动指定case3）：**
```bash
# 仅在需要检测case3时手动指定
python run_detection.py --iteration 5 --scanned-case 3
python run_labeling.py --iteration 5 --scanned-case 3
```

**优势：**
- ✅ 自动与上一轮检测结果保持一致
- ✅ 避免手动指定错误的scanned_case
- ✅ 简化命令行参数

## 核心组件说明

| 文件 | 职责 |
|-----|------|
| [batch_runner.py](batch_runner.py) | 主入口，运行迭代流程 |
| [iteration_controller.py](iteration_controller.py) | 迭代控制逻辑 |
| [task_manager.py](task_manager.py) | 任务扫描和管理 |
| [fp_selector.py](fp_selector.py) | FP选择策略 |
| [run_detection.py](run_detection.py) | 批量检测脚本 |
| [run_labeling.py](run_labeling.py) | 批量标注脚本 |
| [experiment_config.py](experiment_config.py) | 实验配置 |
| [path_utils.py](path_utils.py) | 路径映射工具 |
| [data_structures.py](data_structures.py) | 数据结构定义 |

## 实验输出说明

### task_info.json结构

**Iteration 0:**
```json
{
  "task_id": "pmd_v1_commits/AvoidInstanceof/3/1",
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

**Iteration N (N >= 1):**
```json
{
  "task_id": "pmd_v1_commits/AvoidInstanceof/3/1",
  "iteration": 1,
  "status": "active",
  "fp_selection": {
    "total_fps_in_results": 10,
    "remaining_fps_count": 9,
    "selected_fp": {
      "label": "fp",
      "file_name": "Test.java",
      "function_name": "bar",
      "begin_line": 42,
      "end_line": 55,
      "report_line": 45,
      "method_signature": "public void bar(String arg)",
      "code_snippet": "..."
    }
  },
  "fp_history": {
    "iteration_1": { ... }
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

### iteration_summary.json结构

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

## 故障排查

### 1. 任务无法初始化

**问题：** `Task {task_id}: Missing required input files`

**解决：** 检查以下文件是否存在：
- 原始DSL：`ori_dsl_root/{task_id}.kirin`
- DEF数据：`defs_root/{tool}/{checker}/{group}/{case}/buggy.java`
- 原始检测结果：`ori_detect_results_root/{task_id}_2_labeled_results.json`

### 2. Refine失败

**问题：** `Task {task_id}: Refine failed: ...`

**解决：**
- 检查LLM配置是否正确
- 查看 `refine_context.json` 了解详细错误信息
- 检查DSL语法是否正确

### 3. 检测超时

**问题：** `Task {task_id}: Detection timeout`

**解决：**
- 增加超时时间（在run_detection.py中修改）
- 检查commit代码库大小
- 检查DSL复杂度

## 开发和测试

### 运行单元测试

```bash
# 测试路径映射
pytest tests/exp/test_path_utils.py -v

# 测试实验配置
pytest tests/exp/test_experiment_config.py -v

# 测试迭代流程
pytest tests/exp/test_iteration_flow.py -v

# 测试LLM异步并发
pytest tests/exp/test_llm_async.py -v
```

### 使用小规模数据集测试

```bash
# 只处理前5个任务
python batch_runner.py --iteration 0 --limit 5
python batch_runner.py --iteration 1 --limit 5
```

## 参考文档

- [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) - 详细的实验设计文档
- [data_structures.py](data_structures.py) - 数据结构定义和说明
