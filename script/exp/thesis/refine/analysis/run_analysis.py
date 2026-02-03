"""
跨迭代分析工具 - 命令行入口

用法:
    python -m exp.thesis.refine.analysis.run_analysis \
        --max-iteration 5 \
        --output-dir ./reports
"""

import argparse
from pathlib import Path

from exp.thesis.refine.experiment_config import ExperimentConfig
from exp.thesis.refine.path_utils import PathMapper
from exp.thesis.refine.analysis.evolution_analyzer import EvolutionAnalyzer
from exp.thesis.refine.analysis.visualization import EvolutionVisualizer
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def main():
    """主函数"""
    parser = argparse.ArgumentParser(
        description='DSL迭代优化 - 跨迭代分析工具',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  # 分析5轮迭代的实验结果
  python -m exp.thesis.refine.analysis.run_analysis --max-iteration 5 --output-dir ./reports

  # 分析10轮迭代(准备论文图表)
  python -m exp.thesis.refine.analysis.run_analysis --max-iteration 10 --output-dir ./paper_figures
        """
    )

    # 必需参数
    parser.add_argument(
        '-n', '--max-iteration',
        type=int,
        required=True,
        help='最大迭代轮次 (例如: 5表示分析iteration_0到iteration_5)'
    )
    parser.add_argument(
        '-o', '--output-dir',
        type=str,
        default="./exp/thesis/refine/analysis/out",
        help='输出目录路径 (默认: ./exp/thesis/refine/analysis/out)'
    )

    args = parser.parse_args()

    # 参数验证
    if args.max_iteration < 0:
        _logger.error("max_iteration must be >= 0")
        return 1

    output_dir = Path(args.output_dir)

    _logger.info("=" * 80)
    _logger.info("DSL迭代优化 - 跨迭代分析工具")
    _logger.info("=" * 80)
    _logger.info(f"Max iteration: {args.max_iteration}")
    _logger.info(f"Output directory: {output_dir}")

    # 初始化组件
    _logger.info("Initializing components...")
    config = ExperimentConfig()
    path_mapper = PathMapper(config)
    analyzer = EvolutionAnalyzer(path_mapper)
    visualizer = EvolutionVisualizer()

    # 执行全局演化分析
    _logger.info("Analyzing global evolution...")
    report = analyzer.analyze_global_evolution(args.max_iteration)

    if report.total_tasks == 0:
        _logger.error("No tasks found. Please check if iteration_0 exists and contains task data.")
        return 1

    # 保存JSON报告
    json_output_path = output_dir / "global_evolution.json"
    report.save_to_file(json_output_path)
    _logger.info(f"JSON report saved to: {json_output_path}")

    # 生成可视化图表
    _logger.info("Generating visualization...")
    chart_output_path = output_dir / "global_overview"
    visualizer.plot_global_overview(report, chart_output_path)

    # 输出统计摘要
    _logger.info("=" * 80)
    _logger.info("分析完成!")
    _logger.info("=" * 80)
    _logger.info(f"总任务数: {report.total_tasks}")
    _logger.info(f"分析迭代轮次: 0 → {report.max_iteration}")

    if report.overall_improvement:
        imp = report.overall_improvement
        _logger.info(f"整体改进:")
        _logger.info(f"  Precision: {imp.precision_delta:+.4f}")
        _logger.info(f"  Recall:    {imp.recall_delta:+.4f}")
        _logger.info(f"  F1-Score:  {imp.f1_delta:+.4f}")
        _logger.info(f"  FP减少率:  {imp.avg_fp_reduction_rate:.2%}")

    _logger.info(f"\n输出文件:")
    _logger.info(f"  - JSON报告: {json_output_path}")
    _logger.info(f"  - 可视化图表: {chart_output_path.with_suffix('.png')}")

    return 0


if __name__ == '__main__':
    exit(main())
