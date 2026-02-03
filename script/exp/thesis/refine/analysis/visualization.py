"""
可视化模块 - 生成跨迭代分析图表

使用matplotlib生成3个子图的综合分析图表
"""

from pathlib import Path
import matplotlib.pyplot as plt
import matplotlib
from typing import List

from exp.thesis.refine.analysis.data_structures import GlobalEvolutionReport, GlobalIterationStats
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)

# 配置中文字体(Windows环境)
try:
    matplotlib.rcParams['font.sans-serif'] = ['SimHei']  # 黑体
    matplotlib.rcParams['axes.unicode_minus'] = False  # 解决负号显示问题
except Exception as e:
    _logger.warning(f"Failed to set Chinese font: {e}")


class EvolutionVisualizer:
    """演化可视化器 - 生成matplotlib图表"""

    def __init__(self):
        """初始化可视化器"""
        pass

    def plot_global_overview(
        self,
        report: GlobalEvolutionReport,
        output_path: Path
    ):
        """
        生成全局概览图表(3个子图)

        Args:
            report: GlobalEvolutionReport对象
            output_path: 输出图片路径(不含扩展名,会自动生成.png)
        """
        _logger.info("Generating global overview chart...")

        # 创建输出目录
        output_path.parent.mkdir(parents=True, exist_ok=True)

        # 提取数据
        iterations = [stat.iteration for stat in report.iteration_stats]
        precisions = [stat.avg_precision for stat in report.iteration_stats]
        recalls = [stat.avg_recall for stat in report.iteration_stats]
        f1_scores = [stat.avg_f1 for stat in report.iteration_stats]
        fp_counts = [stat.avg_fp_count for stat in report.iteration_stats]

        # 提取refine指标(仅iteration >= 1有值)
        refine_iterations = []
        tokens = []
        times = []
        for stat in report.iteration_stats:
            if stat.iteration >= 1 and stat.total_tokens is not None:
                refine_iterations.append(stat.iteration)
                tokens.append(stat.total_tokens / 1000)  # 转换为千tokens
                times.append(stat.total_time_seconds)

        # 创建画布: 3个子图,竖向排列
        fig, axes = plt.subplots(3, 1, figsize=(12, 12))

        # 标题(包含工具分布信息)
        title = f'DSL迭代优化 - 全局演化分析 (共{report.total_tasks}个任务, {report.max_iteration}轮迭代)'
        if report.tool_breakdown:
            tool_info = ", ".join([f"{tool.upper()}: {count}" for tool, count in report.tool_breakdown.items()])
            title += f'\n[{tool_info}]'

        fig.suptitle(title, fontsize=16, fontweight='bold')

        # 子图1: 全局指标变化趋势
        ax1 = axes[0]
        ax1.plot(iterations, precisions, marker='o', label='Precision (All)', linewidth=2, markersize=6)
        ax1.plot(iterations, recalls, marker='s', label='Recall (All)', linewidth=2, markersize=6)
        ax1.plot(iterations, f1_scores, marker='^', label='F1-Score (All)', linewidth=2, markersize=6)

        # 添加分工具F1曲线(如果有工具分解数据)
        if report.tool_breakdown:
            # 提取PMD和CodeQL的F1数据
            for tool in ["pmd", "codeql"]:
                tool_f1s = []
                tool_iters = []
                for stat in report.iteration_stats:
                    if stat.tool_stats and tool in stat.tool_stats:
                        tool_f1s.append(stat.tool_stats[tool]["avg_f1"])
                        tool_iters.append(stat.iteration)

                if tool_f1s:
                    linestyle = '--' if tool == "codeql" else ':'
                    ax1.plot(
                        tool_iters,
                        tool_f1s,
                        marker='d',
                        label=f'F1-Score ({tool.upper()})',
                        linewidth=1.5,
                        markersize=5,
                        linestyle=linestyle,
                        alpha=0.7
                    )

        ax1.set_xlabel('迭代轮次', fontsize=12)
        ax1.set_ylabel('指标值', fontsize=12)
        ax1.set_title('全局指标变化趋势', fontsize=14, fontweight='bold')
        ax1.legend(loc='best', fontsize=9)
        ax1.grid(True, alpha=0.3)
        ax1.set_ylim([0, 1.05])

        # 子图2: FP收敛分析
        ax2 = axes[1]
        ax2.plot(iterations, fp_counts, marker='o', color='red', linewidth=2, markersize=6, label='All')

        # 添加分工具FP曲线(如果有工具分解数据)
        if report.tool_breakdown:
            colors = {"pmd": "blue", "codeql": "green"}
            for tool in ["pmd", "codeql"]:
                tool_fps = []
                tool_iters = []
                for stat in report.iteration_stats:
                    if stat.tool_stats and tool in stat.tool_stats:
                        tool_fps.append(stat.tool_stats[tool]["avg_fp_count"])
                        tool_iters.append(stat.iteration)

                if tool_fps:
                    ax2.plot(
                        tool_iters,
                        tool_fps,
                        marker='s',
                        color=colors.get(tool, 'gray'),
                        linewidth=1.5,
                        markersize=5,
                        linestyle='--',
                        alpha=0.7,
                        label=tool.upper()
                    )

        ax2.set_xlabel('迭代轮次', fontsize=12)
        ax2.set_ylabel('平均FP数量', fontsize=12)
        ax2.set_title('FP收敛分析', fontsize=14, fontweight='bold')
        ax2.legend(loc='best', fontsize=10)
        ax2.grid(True, alpha=0.3)

        # 子图3: 资源消耗统计(双Y轴)
        ax3 = axes[2]
        if refine_iterations:
            # 左Y轴: token消耗
            color = 'tab:blue'
            ax3.set_xlabel('迭代轮次', fontsize=12)
            ax3.set_ylabel('Token消耗 (千tokens)', fontsize=12, color=color)
            ax3.bar(
                refine_iterations,
                tokens,
                width=0.4,
                label='Token消耗',
                color=color,
                alpha=0.6
            )
            ax3.tick_params(axis='y', labelcolor=color)
            ax3.legend(loc='upper left', fontsize=10)

            # 右Y轴: 时间消耗
            ax3_right = ax3.twinx()
            color = 'tab:orange'
            ax3_right.set_ylabel('时间消耗 (秒)', fontsize=12, color=color)
            ax3_right.plot(
                refine_iterations,
                times,
                marker='o',
                color=color,
                linewidth=2,
                markersize=6,
                label='时间消耗'
            )
            ax3_right.tick_params(axis='y', labelcolor=color)
            ax3_right.legend(loc='upper right', fontsize=10)

            ax3.set_title('资源消耗统计 (Iteration ≥ 1)', fontsize=14, fontweight='bold')
            ax3.grid(True, alpha=0.3)
        else:
            ax3.text(
                0.5, 0.5,
                '无refine数据\n(仅有iteration_0)',
                ha='center', va='center',
                fontsize=14,
                color='gray'
            )
            ax3.set_title('资源消耗统计', fontsize=14, fontweight='bold')

        # 调整布局
        plt.tight_layout()

        # 保存图片
        output_file = output_path.with_suffix('.png')
        plt.savefig(output_file, dpi=300, bbox_inches='tight')
        _logger.info(f"Chart saved to {output_file}")

        plt.close(fig)

        # 添加改进汇总到日志
        if report.overall_improvement:
            improvement = report.overall_improvement
            _logger.info(
                f"Overall Improvement: "
                f"Precision Δ={improvement.precision_delta:+.4f}, "
                f"Recall Δ={improvement.recall_delta:+.4f}, "
                f"F1 Δ={improvement.f1_delta:+.4f}, "
                f"FP Reduction Rate={improvement.avg_fp_reduction_rate:.2%}"
            )
