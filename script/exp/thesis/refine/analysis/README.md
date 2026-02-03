# DSL迭代优化 - 跨迭代分析工具

## 概述

该分析包提供对DSL迭代优化实验的跨迭代分析功能,通过可视化展示整体实验效果。

## 功能特性

### 全局演化分析
追踪所有任务从iteration_0到iteration_N的整体指标变化趋势,生成综合分析图表。

### 可视化图表(3个子图)

1. **全局指标变化趋势图** - 展示precision/recall/f1随迭代的变化
   - 目的: 评估整体优化效果是否提升
   - 横轴: 迭代轮次 (0 → N)
   - 纵轴: 指标值 (0.0 → 1.0)

2. **FP收敛分析图** - 展示平均FP数量随迭代的变化
   - 目的: 评估误报是否被有效消除
   - 横轴: 迭代轮次 (0 → N)
   - 纵轴: 平均FP数量

3. **资源消耗统计图** - 展示每轮迭代的token和时间消耗
   - 目的: 评估优化成本
   - 横轴: 迭代轮次 (1 → N, iteration_0无refine)
   - 左纵轴: token消耗, 右纵轴: 时间(秒)

---

## 使用方法

### 命令行

```bash
# 长格式
python -m exp.thesis.refine.analysis.run_analysis \
  --max-iteration 5 \
  --output-dir ./reports

# 短格式（推荐）
python -m exp.thesis.refine.analysis.run_analysis -n 5 -o ./reports
```

### 参数说明

- `-n, --max-iteration` (必需) - 分析的最大迭代轮次
  - 示例: `-n 5` 表示分析 iteration_0 到 iteration_5
  - 应该匹配你实际运行的迭代轮次数

- `-o, --output-dir` (可选) - 输出目录路径
  - 示例: `-o ./analysis_reports`
  - 默认: `./exp/thesis/refine/analysis/out`

### 输出文件

```
{output_dir}/
├── global_evolution.json     # 全局演化数据报告(JSON格式)
└── global_overview.png       # 全局概览图表(3个子图)
```

---

## 使用示例

### 示例1: 分析5轮迭代实验
```bash
# 使用短格式
python -m exp.thesis.refine.analysis.run_analysis -n 5

# 或指定输出目录
python -m exp.thesis.refine.analysis.run_analysis -n 5 -o ./reports
```

查看生成的图表,回答:
- 整体指标是否提升? (看趋势图)
- FP是否被有效消除? (看FP收敛图)
- 资源消耗是否合理? (看资源统计图)

### 示例2: 准备论文图表
```bash
python -m exp.thesis.refine.analysis.run_analysis -n 10 -o ./paper_figures
```

输出的PNG图表特点:
- 300 DPI高清晰度
- 清晰的坐标轴标签和图例
- 适合直接插入论文


---

## 数据结构

### GlobalEvolutionReport (JSON格式)

```json
{
  "total_tasks": 50,
  "max_iteration": 5,
  "iteration_stats": [
    {
      "iteration": 0,
      "active_tasks": 50,
      "avg_precision": 0.67,
      "avg_recall": 0.85,
      "avg_f1": 0.75,
      "avg_fp_count": 8.5
    },
    {
      "iteration": 1,
      "active_tasks": 45,
      "avg_precision": 0.72,
      "avg_recall": 0.87,
      "avg_f1": 0.79,
      "avg_fp_count": 6.2,
      "total_tokens": 250000,
      "total_time_seconds": 450.5
    }
  ],
  "overall_improvement": {
    "precision_delta": 0.15,
    "recall_delta": 0.05,
    "f1_delta": 0.12,
    "avg_fp_reduction_rate": 0.65
  }
}
```

---

## 实现细节

### 数据来源
从以下路径读取迭代实验数据:
```
iterExp/
├── iteration_0/
│   ├── iteration_summary.json
│   └── {task_id}/
│       └── task_info.json
├── iteration_1/
│   └── ...
└── iteration_N/
    └── ...
```

### 核心模块
- `data_structures.py` - 定义GlobalEvolutionReport等数据结构
- `evolution_analyzer.py` - 实现EvolutionAnalyzer类,负责加载和聚合数据
- `visualization.py` - 实现EvolutionVisualizer类,负责生成matplotlib图表
- `run_analysis.py` - 命令行入口

### 设计原则
- **独立包设计** - 与现有refine代码完全分离,仅读取结果文件
- **简洁实用** - 聚焦核心功能,保持代码简单
- **易于扩展** - 后续可按需添加更多分析维度

---

## 后续可选扩展

如有需要,可以增加以下功能:
1. 单任务详细分析
2. 多任务对比分析
3. 实时监控集成
4. 交互式Web Dashboard
