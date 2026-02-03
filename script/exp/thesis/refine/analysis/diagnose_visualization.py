"""
诊断可视化模块 - 生成效果差的任务的分析图表

使用matplotlib生成问题任务的详细分析图表
"""

from pathlib import Path
import matplotlib.pyplot as plt
import matplotlib
from typing import List, Dict

from exp.thesis.refine.analysis.diagnose_bad_cases import DiagnosisReport, TaskDiagnosis
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)

# 配置中文字体(Windows环境)
try:
    matplotlib.rcParams['font.sans-serif'] = ['SimHei']  # 黑体
    matplotlib.rcParams['axes.unicode_minus'] = False  # 解决负号显示问题
except Exception as e:
    _logger.warning(f"Failed to set Chinese font: {e}")


class DiagnosisVisualizer:
    """诊断可视化器 - 生成问题任务分析图表"""

    def __init__(self):
        """初始化可视化器"""
        pass

    def plot_diagnosis_overview(
        self,
        report: DiagnosisReport,
        output_path: Path
    ):
        """
        生成诊断概览图表(4个子图)

        Args:
            report: DiagnosisReport对象
            output_path: 输出图片路径(不含扩展名,会自动生成.png)
        """
        _logger.info("Generating diagnosis overview chart...")

        # 创建输出目录
        output_path.parent.mkdir(parents=True, exist_ok=True)

        # 创建画布: 4个子图,2x2排列
        fig, axes = plt.subplots(2, 2, figsize=(16, 12))

        # 标题
        title = f'DSL迭代优化 - 问题任务诊断 (iteration {report.iterations_analyzed[0]} → {report.iterations_analyzed[1]})'
        fig.suptitle(title, fontsize=16, fontweight='bold')

        # 子图1: 问题类型分布(饼图)
        ax1 = axes[0, 0]
        self._plot_problem_type_distribution(ax1, report)

        # 子图2: 问题任务按工具分布(堆叠柱状图)
        ax2 = axes[0, 1]
        self._plot_problem_by_tool(ax2, report)

        # 子图3: 召回率下降分布(散点图)
        ax3 = axes[1, 0]
        self._plot_recall_drop_scatter(ax3, report)

        # 子图4: 精确率下降分布(散点图)
        ax4 = axes[1, 1]
        self._plot_precision_drop_scatter(ax4, report)

        # 调整布局
        plt.tight_layout()

        # 保存图片
        output_file = output_path.with_suffix('.png')
        plt.savefig(output_file, dpi=300, bbox_inches='tight')
        _logger.info(f"Chart saved to {output_file}")

        plt.close(fig)

    def _plot_problem_type_distribution(self, ax, report: DiagnosisReport):
        """子图1: 问题类型分布(饼图)"""
        counts = report.summary['problem_counts']

        labels = []
        sizes = []
        colors = ['#ff6b6b', '#feca57', '#48dbfb', '#c8d6e5']

        type_labels = {
            'syntax_error': '语法错误',
            'recall_drop': '召回率下降',
            'precision_drop': '精确率下降',
            'no_improvement': '无改进'
        }

        for problem_type, label in type_labels.items():
            count = counts[problem_type]
            if count > 0:
                labels.append(f'{label}\n({count})')
                sizes.append(count)

        if sizes:
            ax.pie(
                sizes,
                labels=labels,
                colors=colors[:len(sizes)],
                autopct='%1.1f%%',
                startangle=90,
                textprops={'fontsize': 10}
            )
            ax.set_title('问题类型分布', fontsize=12, fontweight='bold')
        else:
            ax.text(
                0.5, 0.5,
                '无问题任务',
                ha='center', va='center',
                fontsize=14,
                color='green'
            )
            ax.set_title('问题类型分布', fontsize=12, fontweight='bold')

    def _plot_problem_by_tool(self, ax, report: DiagnosisReport):
        """子图2: 问题任务按工具分布(堆叠柱状图)"""
        problem_tool_breakdown = report.summary['problem_tool_breakdown']

        categories = ['语法错误', '召回率下降', '精确率下降', '无改进']
        problem_keys = ['syntax_error', 'recall_drop', 'precision_drop', 'no_improvement']

        pmd_counts = [problem_tool_breakdown[key]['pmd'] for key in problem_keys]
        codeql_counts = [problem_tool_breakdown[key]['codeql'] for key in problem_keys]

        x = range(len(categories))
        width = 0.35

        # 绘制堆叠柱状图
        ax.bar(x, pmd_counts, width, label='PMD', color='#3498db')
        ax.bar(x, codeql_counts, width, bottom=pmd_counts, label='CodeQL', color='#2ecc71')

        ax.set_xlabel('问题类型', fontsize=11)
        ax.set_ylabel('任务数', fontsize=11)
        ax.set_title('问题任务按工具分布', fontsize=12, fontweight='bold')
        ax.set_xticks(x)
        ax.set_xticklabels(categories, fontsize=9)
        ax.legend(loc='upper right', fontsize=10)
        ax.grid(True, alpha=0.3, axis='y')

    def _plot_recall_drop_scatter(self, ax, report: DiagnosisReport):
        """子图3: 召回率下降分布(散点图)"""
        recall_drop_tasks = report.recall_drop_tasks

        if not recall_drop_tasks:
            ax.text(
                0.5, 0.5,
                '无召回率下降任务',
                ha='center', va='center',
                fontsize=14,
                color='green'
            )
            ax.set_title('召回率下降分析', fontsize=12, fontweight='bold')
            return

        # 提取数据
        initial_recalls = [t.initial_recall for t in recall_drop_tasks]
        recall_deltas = [t.recall_delta for t in recall_drop_tasks]
        fp_deltas = [t.fp_delta for t in recall_drop_tasks]

        # 按工具着色
        colors = []
        for task in recall_drop_tasks:
            colors.append('#3498db' if task.tool == 'pmd' else '#2ecc71')

        # 按FP变化确定大小
        sizes = [max(50, abs(fp_delta) * 20) for fp_delta in fp_deltas]

        # 绘制散点图
        ax.scatter(initial_recalls, recall_deltas, s=sizes, c=colors, alpha=0.6, edgecolors='black', linewidth=0.5)

        ax.set_xlabel('初始召回率', fontsize=11)
        ax.set_ylabel('召回率变化', fontsize=11)
        ax.set_title('召回率下降分析 (气泡大小=FP变化)', fontsize=12, fontweight='bold')
        ax.axhline(y=0, color='red', linestyle='--', linewidth=1, alpha=0.5)
        ax.grid(True, alpha=0.3)

        # 添加图例
        from matplotlib.lines import Line2D
        legend_elements = [
            Line2D([0], [0], marker='o', color='w', markerfacecolor='#3498db', markersize=8, label='PMD'),
            Line2D([0], [0], marker='o', color='w', markerfacecolor='#2ecc71', markersize=8, label='CodeQL')
        ]
        ax.legend(handles=legend_elements, loc='lower left', fontsize=9)

    def _plot_precision_drop_scatter(self, ax, report: DiagnosisReport):
        """子图4: 精确率下降分布(散点图)"""
        precision_drop_tasks = report.precision_drop_tasks

        if not precision_drop_tasks:
            ax.text(
                0.5, 0.5,
                '无精确率下降任务',
                ha='center', va='center',
                fontsize=14,
                color='green'
            )
            ax.set_title('精确率下降分析', fontsize=12, fontweight='bold')
            return

        # 提取数据
        initial_precisions = [t.initial_precision for t in precision_drop_tasks]
        precision_deltas = [t.precision_delta for t in precision_drop_tasks]
        fp_deltas = [t.fp_delta for t in precision_drop_tasks]

        # 按工具着色
        colors = []
        for task in precision_drop_tasks:
            colors.append('#3498db' if task.tool == 'pmd' else '#2ecc71')

        # 按FP变化确定大小
        sizes = [max(50, abs(fp_delta) * 20) for fp_delta in fp_deltas]

        # 绘制散点图
        ax.scatter(initial_precisions, precision_deltas, s=sizes, c=colors, alpha=0.6, edgecolors='black', linewidth=0.5)

        ax.set_xlabel('初始精确率', fontsize=11)
        ax.set_ylabel('精确率变化', fontsize=11)
        ax.set_title('精确率下降分析 (气泡大小=FP变化)', fontsize=12, fontweight='bold')
        ax.axhline(y=0, color='red', linestyle='--', linewidth=1, alpha=0.5)
        ax.grid(True, alpha=0.3)

        # 添加图例
        from matplotlib.lines import Line2D
        legend_elements = [
            Line2D([0], [0], marker='o', color='w', markerfacecolor='#3498db', markersize=8, label='PMD'),
            Line2D([0], [0], marker='o', color='w', markerfacecolor='#2ecc71', markersize=8, label='CodeQL')
        ]
        ax.legend(handles=legend_elements, loc='lower left', fontsize=9)

    def plot_top_bad_cases(
        self,
        report: DiagnosisReport,
        output_path: Path,
        top_n: int = 20
    ):
        """
        生成Top N最差任务对比图

        Args:
            report: DiagnosisReport对象
            output_path: 输出图片路径
            top_n: 显示的最差任务数量
        """
        _logger.info(f"Generating top {top_n} bad cases chart...")

        # 创建输出目录
        output_path.parent.mkdir(parents=True, exist_ok=True)

        # 收集所有问题任务
        all_problem_tasks = (
            report.syntax_error_tasks +
            report.recall_drop_tasks +
            report.precision_drop_tasks +
            report.no_improvement_tasks
        )

        if not all_problem_tasks:
            _logger.warning("No problem tasks found, skipping chart")
            return

        # 按F1 delta排序(越负越严重)
        all_problem_tasks.sort(key=lambda t: t.f1_delta)

        # 取前N个
        top_tasks = all_problem_tasks[:top_n]

        # 创建画布
        fig, ax = plt.subplots(figsize=(14, max(8, len(top_tasks) * 0.4)))

        # 提取数据
        task_labels = []
        f1_deltas = []
        colors = []

        severity_colors = {
            'critical': '#e74c3c',
            'high': '#e67e22',
            'medium': '#f39c12',
            'normal': '#95a5a6'
        }

        for task in top_tasks:
            # 简化task_id显示(只显示checker/group/case)
            parts = task.task_id.split('/')
            if len(parts) >= 3:
                short_label = '/'.join(parts[-3:])
            else:
                short_label = task.task_id

            task_labels.append(f"{short_label}\n[{task.problem_type}]")
            f1_deltas.append(task.f1_delta)
            colors.append(severity_colors[task.severity])

        # 绘制水平柱状图
        y_pos = range(len(task_labels))
        ax.barh(y_pos, f1_deltas, color=colors, edgecolor='black', linewidth=0.5)

        ax.set_yticks(y_pos)
        ax.set_yticklabels(task_labels, fontsize=8)
        ax.set_xlabel('F1-Score变化', fontsize=12)
        ax.set_title(f'Top {len(top_tasks)} 效果最差任务', fontsize=14, fontweight='bold')
        ax.axvline(x=0, color='red', linestyle='--', linewidth=1, alpha=0.5)
        ax.grid(True, alpha=0.3, axis='x')

        # 添加图例
        from matplotlib.patches import Patch
        legend_elements = [
            Patch(facecolor='#e74c3c', label='Critical'),
            Patch(facecolor='#e67e22', label='High'),
            Patch(facecolor='#f39c12', label='Medium'),
            Patch(facecolor='#95a5a6', label='Normal')
        ]
        ax.legend(handles=legend_elements, loc='lower right', fontsize=10)

        plt.tight_layout()

        # 保存图片
        output_file = output_path.with_suffix('.png')
        plt.savefig(output_file, dpi=300, bbox_inches='tight')
        _logger.info(f"Chart saved to {output_file}")

        plt.close(fig)
