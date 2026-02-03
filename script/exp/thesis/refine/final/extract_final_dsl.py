"""
提取最终版本DSL工具

功能：
1. 遍历所有任务，检查每个迭代的DSL文件是否存在
2. 比较有DSL文件的迭代中F1最高的
3. 复制最佳DSL到final_version目录
"""

import argparse
import json
import shutil
from pathlib import Path
from typing import Optional, List

from exp.thesis.refine.experiment_config import ExperimentConfig
from exp.thesis.refine.path_utils import PathMapper
from exp.thesis.refine.utils import infer_scanned_case_for_task
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def get_all_task_ids(path_mapper: PathMapper, max_iteration: int) -> List[str]:
    """
    获取所有任务ID（从origin DSL目录扫描）

    这样可以确保提取所有origin DSL对应的最佳版本，
    而不只是在迭代实验中有结果的任务。
    """
    _logger.info("Scanning origin DSL directory for all tasks...")
    all_task_ids = path_mapper.scan_dsl_files()
    _logger.info(f"Found {len(all_task_ids)} tasks in origin DSL directory")
    return all_task_ids


def find_best_dsl_iteration(
    path_mapper: PathMapper,
    task_id: str,
    max_iteration: int,
    use_origin_fallback: bool = True
) -> Optional[tuple]:
    """
    找到任务的最佳DSL迭代

    Args:
        path_mapper: 路径映射器
        task_id: 任务ID
        max_iteration: 最大迭代轮次
        use_origin_fallback: 如果找不到迭代结果，是否使用origin DSL作为fallback

    Returns:
        (best_iteration, best_f1, dsl_path, scanned_case) 如果找到，否则None
    """
    # 解析task_id获取case名称
    _, _, _, case = path_mapper.parse_task_id(task_id)

    best_f1 = -1.0
    best_iteration = -1
    best_dsl_path = None
    best_scanned_case = None

    # 遍历所有迭代
    for iteration in range(max_iteration + 1):
        # 推断scanned_case（用于目标文件名）
        scanned_case = infer_scanned_case_for_task(task_id, iteration, path_mapper)
        if not scanned_case:
            _logger.debug(f"Task {task_id} iteration {iteration}: Failed to infer scanned_case, skip")
            continue

        # 检查DSL文件是否存在（原始文件名只是 {case}.kirin）
        output_dir = path_mapper.get_iteration_output_dir(task_id, iteration)
        dsl_path = output_dir / "dsl.kirin"

        if not dsl_path.exists():
            # DSL不存在，跳过这个迭代
            _logger.debug(f"Task {task_id} iteration {iteration}: DSL not found at {dsl_path}")
            continue

        # DSL存在，检查task_info
        task_info_path = path_mapper.get_iteration_task_info_path(task_id, iteration)
        if not task_info_path.exists():
            _logger.warning(f"Task {task_id} iteration {iteration}: DSL exists but task_info missing")
            continue

        # 读取F1
        try:
            with open(task_info_path, 'r', encoding='utf-8') as f:
                task_data = json.load(f)

            # 提取F1
            metrics = task_data.get('metrics', {}).get('detection_metrics')
            if not metrics:
                continue

            f1 = metrics.get('f1_score', 0.0)

            # 更新最佳
            if f1 > best_f1:
                best_f1 = f1
                best_iteration = iteration
                best_dsl_path = dsl_path
                best_scanned_case = scanned_case

        except Exception as e:
            _logger.warning(f"Task {task_id} iteration {iteration}: Failed to read metrics: {e}")
            continue

    # 如果找不到任何有效的迭代结果，使用origin DSL作为fallback
    if best_iteration == -1 and use_origin_fallback:
        origin_dsl_path = path_mapper.get_ori_dsl_path(task_id)
        if origin_dsl_path.exists():
            # 尝试推断scanned_case（通常是"2"）
            # 这里我们需要一个默认的scanned_case，通常是case+1
            try:
                case_num = int(case)
                default_scanned_case = str(case_num + 1)
            except ValueError:
                default_scanned_case = "2"  # 默认值

            _logger.info(f"Task {task_id}: No iteration results, using origin DSL as fallback")
            return (-1, 0.0, origin_dsl_path, default_scanned_case)

    if best_iteration == -1:
        return None

    return (best_iteration, best_f1, best_dsl_path, best_scanned_case)


def copy_best_dsl(
    path_mapper: PathMapper,
    task_id: str,
    best_dsl_path: Path,
    best_scanned_case: str,
    final_root: Path
) -> bool:
    """
    复制最佳DSL到final目录

    Args:
        path_mapper: 路径映射器
        task_id: 任务ID
        best_dsl_path: 最佳DSL源路径
        best_scanned_case: 最佳scanned_case
        final_root: final根目录

    Returns:
        是否成功复制
    """
    # 目标路径：保持task_id的目录结构和原文件名格式
    _, _, _, case = path_mapper.parse_task_id(task_id)
    dst_path = final_root / task_id / f"{case}_{best_scanned_case}.kirin"
    dst_path.parent.mkdir(parents=True, exist_ok=True)

    try:
        shutil.copy2(best_dsl_path, dst_path)
        return True
    except Exception as e:
        _logger.error(f"Task {task_id}: Failed to copy DSL: {e}")
        return False


def main():
    parser = argparse.ArgumentParser(
        description='提取最终版本DSL (每个任务F1最高的版本)',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  # 提取最终版本DSL
  python -m exp.thesis.refine.final.extract_final_dsl -n 5
        """
    )

    parser.add_argument('-n', '--max-iteration', type=int, required=True, help='最大迭代轮次')

    args = parser.parse_args()

    _logger.info("=" * 80)
    _logger.info("提取最终版本DSL工具")
    _logger.info("=" * 80)
    _logger.info(f"Max iteration: {args.max_iteration}")

    # 初始化
    config = ExperimentConfig()
    path_mapper = PathMapper(config)
    final_root = config.final_dsl_root

    final_root.mkdir(parents=True, exist_ok=True)
    _logger.info(f"Output directory: {final_root}")

    # 获取所有任务
    all_task_ids = get_all_task_ids(path_mapper, args.max_iteration)
    _logger.info(f"Found {len(all_task_ids)} tasks")

    # 统计信息
    success_count = 0
    skipped_count = 0
    fallback_count = 0
    total_f1 = 0.0
    iteration_distribution = {}
    skipped_tasks = []

    # 处理每个任务
    for task_id in all_task_ids:
        # 找到最佳DSL迭代
        result = find_best_dsl_iteration(path_mapper, task_id, args.max_iteration, use_origin_fallback=True)

        if not result:
            _logger.warning(f"Task {task_id}: No valid DSL found (even origin DSL missing)")
            skipped_count += 1
            skipped_tasks.append(task_id)
            continue

        best_iteration, best_f1, best_dsl_path, best_scanned_case = result

        # 复制DSL
        if copy_best_dsl(path_mapper, task_id, best_dsl_path, best_scanned_case, final_root):
            if best_iteration == -1:
                # 使用了origin fallback
                _logger.info(f"Task {task_id}: Copied from origin DSL (fallback)")
                fallback_count += 1
            else:
                _logger.info(f"Task {task_id}: Copied from iteration {best_iteration} (F1={best_f1:.4f})")
                total_f1 += best_f1

            success_count += 1

            # 统计迭代分布
            iteration_key = "origin_fallback" if best_iteration == -1 else best_iteration
            iteration_distribution[iteration_key] = iteration_distribution.get(iteration_key, 0) + 1
        else:
            skipped_count += 1
            skipped_tasks.append(task_id)

    _logger.info("=" * 80)
    _logger.info(f"完成! 成功提取 {success_count}/{len(all_task_ids)} 个任务的最终DSL")
    _logger.info(f"  - 从迭代实验提取: {success_count - fallback_count}")
    _logger.info(f"  - 使用origin DSL fallback: {fallback_count}")
    _logger.info(f"跳过: {skipped_count} 个任务")
    _logger.info(f"输出目录: {final_root}")
    _logger.info("=" * 80)

    if skipped_count > 0:
        _logger.warning(f"\n跳过的任务列表 ({skipped_count}):")
        for task in skipped_tasks:
            _logger.warning(f"  × {task}")

    if success_count > 0:
        # 计算平均F1（只针对有迭代结果的任务）
        if success_count - fallback_count > 0:
            avg_f1 = total_f1 / (success_count - fallback_count)
            _logger.info(f"\n平均F1 (有迭代结果的任务): {avg_f1:.4f}")

        _logger.info("\n最佳迭代分布:")
        # 先输出数字迭代
        for iteration in sorted([k for k in iteration_distribution.keys() if isinstance(k, int)]):
            count = iteration_distribution[iteration]
            percentage = count / success_count * 100
            _logger.info(f"  Iteration {iteration}: {count} tasks ({percentage:.1f}%)")
        # 再输出fallback
        if "origin_fallback" in iteration_distribution:
            count = iteration_distribution["origin_fallback"]
            percentage = count / success_count * 100
            _logger.info(f"  Origin fallback: {count} tasks ({percentage:.1f}%)")

    return 0


if __name__ == '__main__':
    exit(main())
