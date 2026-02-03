# Final DSL 和 RQ3 实验

## 概述

该包负责两个主要任务：

1. **提取最终版本DSL**: 从迭代优化实验中提取每个任务F1分数最高的DSL版本
2. **RQ3实验**: 使用最终版本DSL在其他case上进行测试，验证DSL的泛化能力

---

## 工作流程

### Step 1: 提取最终版本DSL

完成迭代优化实验后，运行提取脚本：

```bash
# 提取最终版本DSL (假设最大迭代为5)
python -m exp.thesis.refine.final.extract_final_dsl -n 5
```

**脚本逻辑**:
1. 遍历所有任务和迭代
2. 推断scanned_case（从检测结果文件名中提取）
3. 检查DSL文件是否存在（`{case}_{scanned_case}.kirin`）
4. 如果DSL存在，读取该迭代的F1分数
5. 比较所有有DSL的迭代，选择F1最高的
6. 复制最佳DSL到final_version目录，保持原目录结构和文件名

**输出**:
- `E:/dataset/Navi/final_thesis_datas/final_version/` - 最终版本DSL文件
  - 目录结构: `{dataset}/{checker}/{group}/{case}/{case}_{scanned_case}.kirin`
  - 保持原文件名格式（如`1_2.kirin`表示case 1在case 2上检测）

**输出示例**:
```
================================================================================
完成! 成功提取 223/223 个任务的最终DSL
跳过: 0 个任务
输出目录: E:/dataset/Navi/final_thesis_datas/final_version
================================================================================

平均F1: 0.7855

最佳迭代分布:
  Iteration 0: 164 tasks (73.5%)
  Iteration 1: 35 tasks (15.7%)
  Iteration 2: 15 tasks (6.7%)
  Iteration 3: 7 tasks (3.1%)
  Iteration 4: 2 tasks (0.9%)
```

### Step 2: RQ3实验 - 泛化能力测试

RQ3实验目标：验证优化后的DSL在**其他case**上的泛化能力。

#### 实验设计

对于每个checker/group，我们有多个case (例如case 1, 2, 3, 4)。DSL文件名格式为`{case1}_{case2}.kirin`，表示在case1和case2上训练，现在我们要测试：

- **训练cases**: DSL文件名中的case1和case2 (例如 `1_2.kirin` 表示case 1和case 2)
- **测试cases**: 同一group下的其他cases (例如case 3, 4, 5...)

#### 运行RQ3实验

```bash
# 运行RQ3实验
python -m exp.thesis.refine.final.run_rq3 \
  --engine-path D:/envs/kirin-cli-1.0.8_sp06-jackofext-obfuscate.jar
```

**脚本逻辑**:
1. 扫描final_version目录下的所有DSL文件
2. 从文件名提取训练case1和case2（例如 `1_2.kirin` → case1=1, case2=2）
3. 在commit目录下查找同一group的其他case（排除case1和case2）
4. 对每个测试case运行检测
5. 标注TP/FP/FN并计算指标
6. 保存检测结果到DSL文件所在目录的rq3_results子目录
7. 生成全局汇总报告

**输出结构**:
```
final_version/
├── pmd_v1_commits/
│   ├── AvoidInstanceof/
│   │   └── 3/
│   │       └── 1/
│   │           ├── 1_2.kirin                          # 最终DSL
│   │           └── rq3_results/                       # RQ3测试结果
│   │               ├── test_on_3/                     # 在case 3上的检测结果
│   │               │   └── *.xml
│   │               ├── test_on_3_labeled_results.json # 标注结果
│   │               ├── test_on_4/
│   │               └── test_on_4_labeled_results.json
│   └── ...
├── rq3_summary.json                                   # RQ3全局汇总报告
└── ...
```

**汇总报告示例**:
```json
{
  "total_tasks": 223,
  "tested_tasks": 180,
  "skipped_tasks": 43,
  "failed_tasks": 0,
  "avg_precision": 0.7234,
  "avg_recall": 0.6891,
  "avg_f1": 0.7058,
  "avg_fp_count": 2.45,
  "task_results": [
    {
      "task_id": "pmd_v1_commits/AvoidInstanceof/3/1",
      "dsl_file": "1_2.kirin",
      "train_case1": "1",
      "train_case2": "2",
      "test_case": "3",
      "detection_metrics": {
        "precision": 0.8000,
        "recall": 0.7500,
        "f1_score": 0.7742,
        "fp_count": 2
      },
      "status": "success"
    }
  ]
}
```

---

## 目录结构

```
final/
├── __init__.py
├── README.md                    # 本文档
├── extract_final_dsl.py         # 提取最终版本DSL脚本
└── run_rq3.py                   # RQ3实验脚本 (待实现)
```

---

## 数据目录

### 最终DSL目录
```
E:/dataset/Navi/final_thesis_datas/final_version/
├── pmd_v1_commits/
│   ├── AvoidInstanceof/
│   │   ├── 3/
│   │   │   └── 1/
│   │   │       └── 1_2.kirin        # 最终版本DSL (case 1在case 2上检测)
│   │   └── ...
│   └── ...
└── codeql_v1_commits/
    └── ...
```

### RQ3实验结果目录
```
E:/dataset/Navi/final_thesis_datas/final_version/
├── pmd_v1_commits/
│   ├── {checker}/{group}/{case}/
│   │   ├── {case1}_{case2}.kirin
│   │   └── rq3_results/
│   │       ├── test_on_{test_case}/
│   │       │   └── *.xml
│   │       └── test_on_{test_case}_labeled_results.json
│   └── ...
├── rq3_summary.json                # RQ3全局汇总报告
└── ...
```

---

## 关键指标

### 提取最终DSL
- **提取率**: 成功提取DSL的任务数 / 总任务数
- **平均指标**: 最终版本DSL的平均Precision/Recall/F1
- **迭代分布**: 最佳迭代的分布 (哪些任务在iteration 0就是最佳，哪些需要多轮优化)

### RQ3实验
- **泛化能力**: 测试cases的平均F1分数
- **测试覆盖率**: 成功测试的任务数 / 总任务数
- **平均指标**: 在测试cases上的平均Precision/Recall/F1
- **平均FP**: 在测试cases上的平均FP数量

---

## 已实现功能

### extract_final_dsl.py
- [x] 扫描所有任务和迭代
- [x] 推断scanned_case
- [x] 选择F1最高的迭代
- [x] 复制最佳DSL到final_version目录
- [x] 统计最佳迭代分布

### run_rq3.py
- [x] 扫描final_version目录下的DSL文件
- [x] 从文件名推断训练cases
- [x] 查找可用的测试cases
- [x] 批量运行检测和标注
- [x] 生成RQ3汇总报告
- [x] 计算平均指标

---

## 使用示例

### 示例1: 提取最终版本DSL
```bash
python -m exp.thesis.refine.final.extract_final_dsl -n 5
```

输出示例:
```
================================================================================
提取最终版本DSL工具
================================================================================
Max iteration: 5
Output directory: E:/dataset/Navi/final_thesis_datas/final_version
Found 223 tasks
Task pmd_v1_commits/AvoidInstanceof/3/1: Copied from iteration 0 (F1=0.8571)
Task pmd_v1_commits/AvoidInstanceof/3/2: Copied from iteration 1 (F1=0.9000)
...
================================================================================
完成! 成功提取 223/223 个任务的最终DSL
跳过: 0 个任务
输出目录: E:/dataset/Navi/final_thesis_datas/final_version
================================================================================

平均F1: 0.7855

最佳迭代分布:
  Iteration 0: 164 tasks (73.5%)
  Iteration 1: 35 tasks (15.7%)
  Iteration 2: 15 tasks (6.7%)
  Iteration 3: 7 tasks (3.1%)
  Iteration 4: 2 tasks (0.9%)
```

### 示例2: RQ3实验
```bash
python -m exp.thesis.refine.final.run_rq3 \
  --engine-path D:/envs/kirin-cli-1.0.8_sp06-jackofext-obfuscate.jar
```

输出示例:
```
================================================================================
RQ3实验：测试最终版本DSL的泛化能力
================================================================================
Final DSL root: E:/dataset/Navi/final_thesis_datas/final_version
Commit root: E:/dataset/Navi/rq2_commit
Found 223 DSL files in final_version

[1/223] Processing task: pmd_v1_commits/AvoidInstanceof/3/1
Task pmd_v1_commits/AvoidInstanceof/3/1: Train cases: 1, 2
Task pmd_v1_commits/AvoidInstanceof/3/1: Found 2 test cases: ['3', '4']
Task pmd_v1_commits/AvoidInstanceof/3/1: Testing DSL on case 3
Task pmd_v1_commits/AvoidInstanceof/3/1: TP=15, FP=2, FN=3
...

================================================================================
RQ3实验完成!
================================================================================
总DSL文件数: 223
成功测试: 380
跳过: 43 (无额外测试case)
失败: 0

平均指标（基于成功测试）:
  Precision: 0.7234
  Recall: 0.6891
  F1: 0.7058
  平均FP数量: 2.45

汇总报告: E:/dataset/Navi/final_thesis_datas/final_version/rq3_summary.json
================================================================================
```

---

## 常见问题

### Q1: 如果某个任务在所有迭代中F1都一样怎么办？
A: 选择最早的迭代 (通常是iteration 0)。

### Q2: 如果某个任务没有有效的检测指标怎么办？
A: 跳过该任务，记录警告日志。

### Q3: 最终DSL文件名是什么格式？
A: 格式为`{case}_{scanned_case}.kirin`，例如`1_2.kirin`表示case 1的DSL在case 2上进行检测。scanned_case从检测结果文件名中自动推断。

### Q4: RQ3实验如何确定训练cases和测试cases？
A: 从DSL文件名中提取。例如：
- DSL文件名: `1_2.kirin` → 训练cases: 1和2
- 在同一group下查找其他cases → 测试cases: 3, 4, 5...
- 如果只有训练cases，则跳过该任务

### Q5: 如果某个checker/group只有训练cases怎么办？
A: 该任务会被跳过（status="skipped"），并在汇总报告中标记。

### Q6: RQ3测试结果保存在哪里？
A: 保存在DSL文件所在目录的`rq3_results`子目录下，每个测试case有独立的检测结果和标注文件。
