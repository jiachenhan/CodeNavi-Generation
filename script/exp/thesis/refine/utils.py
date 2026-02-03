"""
工具函数模块

提供通用的辅助函数。
"""
from pathlib import Path
from typing import Optional
import re

from exp.thesis.refine.path_utils import PathMapper
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def extract_scanned_case_from_result_file(result_file_path: Path) -> Optional[str]:
    """
    从检测结果文件名中提取scanned_case

    文件名格式: {dsl_case}_{scanned_case}_labeled_results.json
    示例: 1_2_labeled_results.json -> scanned_case=2

    Args:
        result_file_path: 检测结果文件路径

    Returns:
        scanned_case（如果提取成功），否则返回None
    """
    if not result_file_path.exists():
        return None

    filename = result_file_path.name

    # 匹配模式: {dsl_case}_{scanned_case}_labeled_results.json
    pattern = r"^(\d+)_(\d+)_labeled_results\.json$"
    match = re.match(pattern, filename)

    if match:
        dsl_case = match.group(1)
        scanned_case = match.group(2)
        _logger.debug(f"Extracted scanned_case={scanned_case} from {filename}")
        return scanned_case
    else:
        _logger.warning(f"Failed to extract scanned_case from {filename}")
        return None


def find_scanned_case_from_results_dir(results_dir: Path, dsl_case: str) -> Optional[str]:
    """
    从检测结果目录中查找scanned_case

    Args:
        results_dir: 检测结果目录（iteration_N/{task_id}/detect_results/）
        dsl_case: DSL case编号

    Returns:
        scanned_case（如果找到），否则返回None
    """
    if not results_dir.exists():
        _logger.warning(f"Results directory not found: {results_dir}")
        return None

    # 查找所有 {dsl_case}_*_labeled_results.json 文件
    pattern = f"{dsl_case}_*_labeled_results.json"
    result_files = list(results_dir.glob(pattern))

    if not result_files:
        _logger.warning(f"No result files found matching pattern: {pattern}")
        return None

    if len(result_files) > 1:
        _logger.warning(f"Multiple result files found: {result_files}, using first one")

    # 提取scanned_case
    return extract_scanned_case_from_result_file(result_files[0])


def infer_scanned_case_for_task(
    task_id: str,
    iteration: int,
    path_mapper: PathMapper
) -> Optional[str]:
    """
    为任务推断scanned_case

    推断逻辑：
    1. 如果iteration == 0：从ori_detect_results中查找
    2. 如果iteration >= 1：从上一轮的detect_results中查找
    3. 如果找不到：返回None（让调用方处理）

    Args:
        task_id: 任务ID
        iteration: 迭代轮次
        path_mapper: PathMapper实例

    Returns:
        scanned_case（如果找到），否则返回None
    """
    _, _, _, dsl_case = path_mapper.parse_task_id(task_id)

    if iteration == 0:
        # 从ori_detect_results中查找
        ori_detect_dir = path_mapper.config.ori_detect_results_root / "/".join(task_id.split("/")[:-1])
        scanned_case = find_scanned_case_from_results_dir(ori_detect_dir, dsl_case)
    else:
        # 从上一轮的detect_results中查找
        prev_iteration = iteration - 1
        prev_detect_dir = path_mapper.get_iteration_detect_results_dir(task_id, prev_iteration)
        scanned_case = find_scanned_case_from_results_dir(prev_detect_dir, dsl_case)

    if scanned_case is None:
        _logger.warning(f"Failed to infer scanned_case for task {task_id}")
        return None

    _logger.info(f"Task {task_id}: Inferred scanned_case={scanned_case}")
    return scanned_case
