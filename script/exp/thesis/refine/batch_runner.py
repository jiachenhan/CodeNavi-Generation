"""
批量DSL优化实验运行器

主入口脚本，用于运行迭代式DSL优化实验。

使用方法:
    python batch_runner.py --iteration 0  # 初始化（复制原始DSL）
    python batch_runner.py --iteration 1  # 第一次refine
    python batch_runner.py --iteration 2  # 第二次refine
    ...
"""
import asyncio
import argparse
from pathlib import Path

from exp.thesis.refine.experiment_config import get_default_config, FPSelectionStrategy
from exp.thesis.refine.task_manager import TaskManager
from exp.thesis.refine.fp_selector import FPSelector
from exp.thesis.refine.iteration_controller import IterationController
from interface.llm.llm_pool import AsyncLLMPool
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


async def main():
    """主函数"""
    parser = argparse.ArgumentParser(description="Batch DSL Refinement Runner")
    parser.add_argument(
        "--iteration",
        type=int,
        required=True,
        help="Iteration number (0 for initialization, >=1 for refinement)"
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=None,
        help="Limit the number of tasks to process (for testing)"
    )

    args = parser.parse_args()

    iteration = args.iteration
    limit = args.limit

    if iteration < 0:
        _logger.error("Iteration number must be >= 0")
        return

    _logger.info("=" * 80)
    _logger.info(f"Batch DSL Refinement Runner - Iteration {iteration}")
    _logger.info("=" * 80)

    # 加载配置
    config = get_default_config()
    _logger.info(f"Loaded experiment config")
    _logger.info(f"  Dataset base: {config.dataset_base}")
    _logger.info(f"  LLM pool size: {config.llm_pool_size}")
    _logger.info(f"  FP selection strategy: {config.fp_selection_strategy.value}")

    # 初始化组件
    task_manager = TaskManager(config)
    fp_selector = FPSelector(config.fp_selection_strategy)

    # 创建LLM池
    llm_config = config.get_llm_config()
    api_keys = llm_config["openai"]["api_keys"]
    base_url = llm_config["openai"]["base_url"]
    model = llm_config["openai"]["model"]

    llm_infos = [(base_url, key, model) for key in api_keys[:config.llm_pool_size]]
    llm_pool = AsyncLLMPool(llm_infos)
    _logger.info(f"Created LLM pool with {len(llm_infos)} instances")

    # 创建迭代控制器
    controller = IterationController(config, task_manager, fp_selector, llm_pool)

    # 执行迭代
    if iteration == 0:
        _logger.info("Running Iteration 0: Initialization")
        summary = await controller.run_iteration_0()
    else:
        _logger.info(f"Running Iteration {iteration}: Optimization")
        summary = await controller.run_iteration_n(iteration)

    # 打印结果
    _logger.info("=" * 80)
    _logger.info("Iteration Summary")
    _logger.info("=" * 80)
    _logger.info(f"Total tasks: {summary.total_tasks}")
    _logger.info(f"Active tasks: {summary.active_tasks}")
    _logger.info(f"Stopped tasks: {summary.stopped_tasks}")
    if iteration > 0:
        _logger.info(f"Refine success: {summary.refine_success_count}")
        _logger.info(f"Refine failed: {summary.refine_failed_count}")
        _logger.info(f"Total tokens: {summary.total_tokens}")
    _logger.info(f"Total time: {summary.total_time_seconds}s")

    if summary.stop_reasons:
        _logger.info("Stop reasons:")
        for reason, count in summary.stop_reasons.items():
            _logger.info(f"  {reason}: {count}")

    # 提示用户下一步操作
    _logger.info("=" * 80)
    if iteration == 0:
        _logger.info("Next steps:")
        _logger.info("  1. Review iteration_0/ directory")
        _logger.info("  2. (Optional) Manually run detection if needed")
        _logger.info(f"  3. Run next iteration: python batch_runner.py --iteration 1")
    else:
        if summary.active_tasks > 0:
            _logger.info("Next steps:")
            _logger.info(f"  1. Run detection: python run_detection.py --iteration {iteration}")
            _logger.info(f"  2. Run labeling: python run_labeling.py --iteration {iteration}")
            _logger.info(f"  3. Run next iteration: python batch_runner.py --iteration {iteration + 1}")
        else:
            _logger.info("All tasks have stopped. Experiment completed!")


if __name__ == "__main__":
    asyncio.run(main())
