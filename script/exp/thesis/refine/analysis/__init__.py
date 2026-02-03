"""
DSL迭代优化跨迭代分析包

该包提供对DSL迭代优化实验的跨迭代分析功能,包括:
- 全局演化分析 - 追踪所有任务的整体指标变化趋势
- 可视化图表生成 - 生成3个子图的综合分析图表

使用方法:
    from exp.thesis.refine.analysis.data_structures import GlobalEvolutionReport
    from exp.thesis.refine.analysis.evolution_analyzer import EvolutionAnalyzer
    from exp.thesis.refine.analysis.visualization import EvolutionVisualizer

命令行工具:
    python -m exp.thesis.refine.analysis.run_analysis \
        --max-iteration 5 \
        --output-dir ./reports

注意: 各模块需要显式导入,不支持从包根目录直接导入
"""

__version__ = "0.1.0"
