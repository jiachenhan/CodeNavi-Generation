"""
高FP任务列表生成工具

功能：找出每个迭代中FP数量较多的任务，输出到文件，方便手动检查
"""

import argparse
import json
from pathlib import Path
from typing import List, Optional

from exp.thesis.refine.experiment_config import ExperimentConfig
from exp.thesis.refine.path_utils import PathMapper
from exp.thesis.refine.data_structures import TaskInfo
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def load_task_info(path_mapper: PathMapper, task_id: str, iteration: int) -> Optional[TaskInfo]:
    """加载task_info"""
    task_info_path = path_mapper.get_iteration_task_info_path(task_id, iteration)

    if not task_info_path.exists():
        return None

    try:
        with open(task_info_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        return TaskInfo.from_dict(data)
    except Exception as e:
        _logger.warning(f"Failed to load {task_id} iteration {iteration}: {e}")
        return None


def get_all_task_ids(path_mapper: PathMapper, max_iteration: int) -> List[str]:
    """获取所有任务ID"""
    all_task_ids = set()

    for iteration in range(max_iteration + 1):
        iteration_dir = path_mapper.config.iter_exp_root / f"iteration_{iteration}"

        if not iteration_dir.exists():
            continue

        for task_info_path in iteration_dir.rglob("task_info.json"):
            case_dir = task_info_path.parent
            relative_path = case_dir.relative_to(iteration_dir)
            task_id = str(relative_path).replace('\\', '/')
            all_task_ids.add(task_id)

    return sorted(all_task_ids)


def main():
    parser = argparse.ArgumentParser(
        description='列出每个迭代中FP数量较多的任务',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  # 列出iteration_0到iteration_1中FP较多的任务
  python -m exp.thesis.refine.analysis.diagnose_bad_cases -i 0 -f 1

  # 指定输出文件和FP阈值
  python -m exp.thesis.refine.analysis.diagnose_bad_cases -i 0 -f 1 -o ./bad_cases.txt --fp-threshold 3

  # 只列出前N个FP最多的任务
  python -m exp.thesis.refine.analysis.diagnose_bad_cases -i 0 -f 1 --top-n 20
        """
    )

    parser.add_argument('-i', '--initial-iteration', type=int, required=True, help='初始迭代')
    parser.add_argument('-f', '--final-iteration', type=int, required=True, help='最终迭代')
    parser.add_argument('-o', '--output', type=str, default='./script/exp/thesis/refine/analysis/out/bad_cases.txt', help='输出文件')
    parser.add_argument('--fp-threshold', type=int, default=2, help='FP数量阈值(默认>=2)')
    parser.add_argument('--top-n', type=int, default=None, help='只输出前N个FP最多的任务(默认全部)')

    args = parser.parse_args()

    _logger.info("=" * 80)
    _logger.info("高FP任务列表生成工具")
    _logger.info("=" * 80)
    _logger.info(f"Initial iteration: {args.initial_iteration}")
    _logger.info(f"Final iteration: {args.final_iteration}")
    _logger.info(f"FP threshold: {args.fp_threshold}")
    _logger.info(f"Top N: {args.top_n if args.top_n else 'All'}")

    # 初始化
    config = ExperimentConfig()
    path_mapper = PathMapper(config)

    # 获取所有任务
    all_task_ids = get_all_task_ids(path_mapper, args.final_iteration)
    _logger.info(f"Found {len(all_task_ids)} tasks")

    # 收集所有迭代中的高FP任务
    # 结构: {iteration: [(task_id, fp_count, metrics), ...]}
    iteration_high_fp_cases = {}

    for iteration in range(args.initial_iteration, args.final_iteration + 1):
        _logger.info(f"Analyzing iteration {iteration}...")
        iteration_cases = []

        for task_id in all_task_ids:
            task_info = load_task_info(path_mapper, task_id, iteration)

            if not task_info:
                continue

            metrics = task_info.metrics.detection_metrics
            if not metrics:
                continue

            # 只收集FP数量超过阈值的任务
            if metrics.fp_count >= args.fp_threshold:
                iteration_cases.append({
                    'task_id': task_id,
                    'fp_count': metrics.fp_count,
                    'precision': metrics.precision,
                    'recall': metrics.recall,
                    'f1_score': metrics.f1_score,
                    'tp_count': metrics.tp_count,
                    'fn_count': metrics.fn_count
                })

        # 按FP数量降序排序
        iteration_cases.sort(key=lambda x: x['fp_count'], reverse=True)

        # 如果设置了top_n，只保留前N个
        if args.top_n:
            iteration_cases = iteration_cases[:args.top_n]

        iteration_high_fp_cases[iteration] = iteration_cases
        _logger.info(f"  Found {len(iteration_cases)} high-FP tasks in iteration {iteration}")

    # 输出到文件
    output_path = Path(args.output)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    # 计算总的高FP任务数（去重）
    all_high_fp_tasks = set()
    for iteration_cases in iteration_high_fp_cases.values():
        for case in iteration_cases:
            all_high_fp_tasks.add(case['task_id'])

    with open(output_path, 'w', encoding='utf-8') as f:
        f.write("=" * 100 + "\n")
        f.write(f"高FP任务列表 (iteration {args.initial_iteration} → {args.final_iteration})\n")
        f.write("=" * 100 + "\n")
        f.write(f"总任务数: {len(all_task_ids)}\n")
        f.write(f"FP阈值: >= {args.fp_threshold}\n")
        f.write(f"高FP任务数（去重）: {len(all_high_fp_tasks)}\n")
        f.write(f"高FP比例: {len(all_high_fp_tasks)/len(all_task_ids):.2%}\n")
        f.write("=" * 100 + "\n\n")

        # 按迭代输出
        for iteration in range(args.initial_iteration, args.final_iteration + 1):
            cases = iteration_high_fp_cases[iteration]

            f.write(f"\n{'='*100}\n")
            f.write(f"Iteration {iteration} - 找到 {len(cases)} 个高FP任务\n")
            f.write(f"{'='*100}\n\n")

            for i, case in enumerate(cases, 1):
                f.write(f"{i}. {case['task_id']}\n")
                f.write(f"   FP数量: {case['fp_count']}\n")
                f.write(f"   TP数量: {case['tp_count']}\n")
                f.write(f"   FN数量: {case['fn_count']}\n")
                f.write(f"   Precision: {case['precision']:.4f}\n")
                f.write(f"   Recall: {case['recall']:.4f}\n")
                f.write(f"   F1: {case['f1_score']:.4f}\n")
                f.write(f"   路径: {path_mapper.get_iteration_output_dir(case['task_id'], iteration)}\n")
                f.write("\n")

    _logger.info("=" * 80)
    _logger.info(f"完成! 找到 {len(all_high_fp_tasks)} 个高FP任务（去重）")
    _logger.info(f"输出文件: {output_path}")
    _logger.info("=" * 80)

    # 打印每轮的统计
    for iteration in range(args.initial_iteration, args.final_iteration + 1):
        cases = iteration_high_fp_cases[iteration]
        if cases:
            _logger.info(f"\nIteration {iteration}: {len(cases)} 个高FP任务")
            _logger.info(f"  Top 5:")
            for i, case in enumerate(cases[:5], 1):
                _logger.info(f"  {i}. {case['task_id']} (FP={case['fp_count']}, F1={case['f1_score']:.4f})")

    return 0


if __name__ == '__main__':
    exit(main())
