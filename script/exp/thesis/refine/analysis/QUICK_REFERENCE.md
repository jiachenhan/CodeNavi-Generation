# 快速参考 - 分析工具命令

## 1. 全局演化分析 (run_analysis.py)

### 最简用法
```bash
# 分析5轮迭代（使用默认输出目录）
python -m exp.thesis.refine.analysis.run_analysis -n 5
```

### 常用命令
```bash
# 指定输出目录
python -m exp.thesis.refine.analysis.run_analysis -n 5 -o ./reports

# 分析10轮迭代（准备论文图表）
python -m exp.thesis.refine.analysis.run_analysis -n 10 -o ./paper_figures
```

### 参数
- `-n` : 最大迭代轮次 **(必需)**
- `-o` : 输出目录 (默认: `./exp/thesis/refine/analysis/out`)

### 输出文件
- `global_evolution.json` - 数据报告
- `global_overview.png` - 可视化图表（3个子图）

---

## 2. 效果变差任务列表 (diagnose_bad_cases.py)

### 最简用法
```bash
# 列出iteration_0到iteration_1效果变差的任务
python -m exp.thesis.refine.analysis.diagnose_bad_cases -i 0 -f 1
```

### 常用命令
```bash
# 指定输出文件
python -m exp.thesis.refine.analysis.diagnose_bad_cases -i 0 -f 1 -o ./bad_cases.txt

# 只列出F1下降超过5%的任务
python -m exp.thesis.refine.analysis.diagnose_bad_cases -i 0 -f 1 --f1-threshold 0.05

# 只列出FP增加的任务
python -m exp.thesis.refine.analysis.diagnose_bad_cases -i 0 -f 1 --fp-threshold 1
```

### 参数
- `-i` : 初始迭代轮次 **(必需)** (通常是0)
- `-f` : 最终迭代轮次 **(必需)**
- `-o` : 输出文件 (默认: `./exp/thesis/refine/analysis/out/bad_cases.txt`)
- `--f1-threshold` : F1下降阈值 (默认: 0.0，任何下降都算)
- `--fp-threshold` : FP增加阈值 (默认: 0，任何增加都算)

### 输出文件
- `bad_cases.txt` - 效果变差的任务列表（包含task_id和文件路径）

---

## 3. 典型工作流

### Step 1: 查看整体效果
```bash
python -m exp.thesis.refine.analysis.run_analysis -n 5
```
**查看**：`./exp/thesis/refine/analysis/out/global_overview.png`

### Step 2: 如果指标下降，列出效果变差的任务
```bash
python -m exp.thesis.refine.analysis.diagnose_bad_cases -i 0 -f 5
```
**查看**：
- `./exp/thesis/refine/analysis/out/bad_cases.txt` - 效果变差的任务列表
- 文件中包含每个任务的指标变化和文件路径，方便手动检查

### Step 3: 手动检查变差的任务
打开 `bad_cases.txt`，查看：
- 哪些任务的F1下降最严重
- 哪些任务的FP反而增加了
- 根据任务路径，手动查看DSL和检测结果，找出问题原因

---

## 4. 长短格式对照表

### run_analysis.py
| 短格式 | 长格式 | 说明 |
|--------|--------|------|
| `-n 5` | `--max-iteration 5` | 最大迭代轮次 |
| `-o ./reports` | `--output-dir ./reports` | 输出目录 |

### diagnose_bad_cases.py
| 短格式 | 长格式 | 说明 |
|--------|--------|------|
| `-i 0` | `--initial-iteration 0` | 初始迭代轮次 |
| `-f 5` | `--final-iteration 5` | 最终迭代轮次 |
| `-o ./bad.txt` | `--output ./bad.txt` | 输出文件 |
| 无短格式 | `--f1-threshold 0.05` | F1下降阈值 |
| 无短格式 | `--fp-threshold 1` | FP增加阈值 |

---

## 5. 常见问题

### Q: 输出文件在哪里？
A:
- `run_analysis.py` 默认输出到 `./exp/thesis/refine/analysis/out/`
- `diagnose_bad_cases.py` 默认输出到 `./exp/thesis/refine/analysis/out/bad_cases.txt`

### Q: 如何只看F1下降最严重的任务？
A: 设置阈值，例如 `--f1-threshold 0.05` (只列出F1下降超过5%的)

### Q: 图表中count为什么减少了？
A: 这是bug，已修复。现在每轮的PMD count和CodeQL count应该保持固定。

### Q: 如何验证统计是否正确？
A: 运行验证脚本：
```bash
python -m exp.thesis.refine.analysis.verify_stats -n 5
```
