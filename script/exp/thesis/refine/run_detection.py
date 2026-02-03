"""
批量检测脚本

用于对指定迭代的refined DSL进行检测。

使用方法:
    python run_detection.py --iteration 1
    python run_detection.py --iteration 2 --scanned-case 3  # 用于RQ4验证
"""
import argparse
from pathlib import Path

from exp.thesis.refine.experiment_config import get_default_config
from exp.thesis.refine.path_utils import PathMapper
from exp.thesis.refine.data_structures import TaskInfo, TaskStatus
from exp.thesis.refine.utils import infer_scanned_case_for_task
from interface.java.run_java_api import kirin_engine
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def run_detection_for_task(
    task_id: str,
    iteration: int,
    scanned_case_override: str,
    path_mapper: PathMapper,
    engine_path: Path
):
    """
    对单个任务运行检测

    Args:
        task_id: 任务ID
        iteration: 迭代轮次
        scanned_case_override: 扫描的case编号（None表示自动推断）
        path_mapper: 路径映射器
        engine_path: Kirin引擎路径
    """
    # 自动推断scanned_case（如果未指定override）
    if scanned_case_override:
        scanned_case = scanned_case_override
        _logger.info(f"Task {task_id}: Using override scanned_case={scanned_case}")
    else:
        scanned_case = infer_scanned_case_for_task(task_id, iteration, path_mapper)
        if scanned_case is None:
            _logger.warning(f"Task {task_id}: Failed to infer scanned_case, skipping")
            return
        _logger.info(f"Task {task_id}: Auto-inferred scanned_case={scanned_case}")
    # 加载task_info
    task_info_path = path_mapper.get_iteration_task_info_path(task_id, iteration)
    if not task_info_path.exists():
        _logger.warning(f"Task {task_id}: task_info.json not found, skipping")
        return

    task_info = TaskInfo.load_from_file(task_info_path)

    if task_info.status == TaskStatus.STOPPED:
        _logger.info(f"Task {task_id}: Already stopped, skipping")
        return

    # 获取DSL路径
    dsl_path = path_mapper.get_iteration_dsl_path(task_id, iteration)
    if not dsl_path.exists():
        _logger.error(f"Task {task_id}: DSL not found at {dsl_path}")
        return

    # 获取commit路径
    dataset, checker_name, group, case = path_mapper.parse_task_id(task_id)

    # 确保scanned_case与dsl_case不同
    if scanned_case == case:
        _logger.error(f"Task {task_id}: scanned_case ({scanned_case}) should differ from dsl_case ({case})")
        return

    commit_path = path_mapper.config.commit_root / dataset / checker_name / group / scanned_case / "after"

    if not commit_path.exists():
        _logger.error(f"Task {task_id}: Commit path not found: {commit_path}")
        return

    # 获取输出路径
    detect_results_dir = path_mapper.get_iteration_detect_results_dir(task_id, iteration)
    detect_results_dir.mkdir(parents=True, exist_ok=True)

    result_file = f"{case}_{scanned_case}"  # 例如: 1_2
    result_path = detect_results_dir / result_file

    # 检查是否已存在
    if result_path.exists() and list(result_path.glob("*.xml")):
        _logger.info(f"Task {task_id}: Detection results already exist, skipping")
        return

    # 运行检测
    _logger.info(f"Task {task_id}: Running detection")
    _logger.info(f"  DSL: {dsl_path}")
    _logger.info(f"  Commit: {commit_path}")
    _logger.info(f"  Output: {result_path}")

    timeout = kirin_engine(3600, engine_path, dsl_path, commit_path, result_path)

    if timeout:
        _logger.error(f"Task {task_id}: Detection timeout")
    else:
        _logger.info(f"Task {task_id}: Detection completed")


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description="Run detection for refined DSLs")
    parser.add_argument(
        "--iteration",
        type=int,
        required=True,
        help="Iteration number"
    )
    parser.add_argument(
        "--scanned-case",
        type=str,
        default=None,
        help="Override scanned case number (default: auto-infer from previous iteration). Use '3' for RQ4 validation."
    )
    parser.add_argument(
        "--engine-path",
        type=str,
        default="D:/envs/kirin-cli-1.0.8_sp06-jackofext-obfuscate.jar",
        help="Path to Kirin engine JAR"
    )

    args = parser.parse_args()

    iteration = args.iteration
    scanned_case_override = args.scanned_case
    engine_path = Path(args.engine_path)

    if not engine_path.exists():
        _logger.error(f"Kirin engine not found: {engine_path}")
        return

    _logger.info("=" * 80)
    _logger.info(f"Batch Detection Runner - Iteration {iteration}")
    _logger.info("=" * 80)

    # 加载配置
    config = get_default_config()
    path_mapper = PathMapper(config)

    # 扫描任务
    task_ids = path_mapper.scan_dsl_files()
    _logger.info(f"Found {len(task_ids)} DSL tasks")

    # 对每个任务运行检测
    for i, task_id in enumerate(task_ids, 1):
        _logger.info(f"[{i}/{len(task_ids)}] Processing task: {task_id}")
        run_detection_for_task(task_id, iteration, scanned_case_override, path_mapper, engine_path)

    _logger.info("=" * 80)
    _logger.info("Detection completed!")
    _logger.info("Next step: Run labeling script")
    _logger.info(f"  python run_labeling.py --iteration {iteration}")


if __name__ == "__main__":
    main()
