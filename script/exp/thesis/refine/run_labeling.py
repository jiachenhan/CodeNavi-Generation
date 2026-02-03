"""
批量标注脚本

用于标注检测结果（TP/FP/FN）并更新task_info.json的detection_metrics。

使用方法:
    python run_labeling.py --iteration 1
    python run_labeling.py --iteration 2 --scanned-case 3  # 用于RQ4验证
"""
import json
import argparse
from pathlib import Path
from typing import List, Tuple
from bs4 import BeautifulSoup

from exp.thesis.refine.experiment_config import get_default_config
from exp.thesis.refine.path_utils import PathMapper
from exp.thesis.refine.data_structures import TaskInfo, TaskStatus, StopReason, DetectionMetrics, get_current_timestamp
from exp.thesis.refine.utils import infer_scanned_case_for_task
from utils.config import LoggerConfig
import shutil

_logger = LoggerConfig.get_logger(__name__)


def parse_navi_warnings(xml_file_path: Path) -> List[Tuple[str, str, int]]:
    """
    解析Navi检测结果XML文件

    Args:
        xml_file_path: XML文件路径

    Returns:
        警告列表 [(file_name, function_name, report_line), ...]
    """
    def reduce_engine_format(s) -> str:
        s = s.strip()
        if s.startswith("<![CDATA[") and s.endswith("]]>"):
            return s[9:-3]
        return s

    ret_warnings = []
    with open(xml_file_path, 'r', encoding='ISO-8859-1') as file:
        xml_content = file.read()

    soup = BeautifulSoup(xml_content, 'xml')
    error_tags = soup.find_all("error")

    for error in error_tags:
        detect_info = error.find("defectInfo")
        if not detect_info:
            continue

        file_name = reduce_engine_format(detect_info.fileName.text) if detect_info.fileName else ""
        related_file_name = file_name.split("\\")[-1].replace("/", "#")

        function_name = reduce_engine_format(detect_info.function.text) if detect_info.function else ""
        report_line = int(detect_info.reportLine.text) if detect_info.reportLine else 0

        ret_warnings.append((related_file_name, function_name, report_line))

    return ret_warnings


def load_ground_truth_warnings(sat_warnings_path: Path) -> List[dict]:
    """
    加载ground truth警告（blame版本的warning list）

    Args:
        sat_warnings_path: sat_warnings.json路径

    Returns:
        警告列表
    """
    if not sat_warnings_path.exists():
        _logger.warning(f"Ground truth warnings not found: {sat_warnings_path}")
        return []

    with open(sat_warnings_path, 'r', encoding='utf-8') as f:
        warnings = json.load(f)

    return warnings


def load_commit_methods(methods_path: Path) -> List[dict]:
    """
    加载commit changed functions信息

    Args:
        methods_path: methods.json路径

    Returns:
        函数列表
    """
    if not methods_path.exists():
        _logger.warning(f"Methods info not found: {methods_path}")
        return []

    with open(methods_path, 'r', encoding='utf-8') as f:
        methods = json.load(f)

    return methods


def copy_from_previous_iteration(
    task_id: str,
    iteration: int,
    scanned_case_override: str,
    path_mapper: PathMapper
) -> bool:
    """
    从上一轮复制DSL、检测结果和task_info（如果上一轮是STOPPED状态）

    Args:
        task_id: 任务ID
        iteration: 当前迭代轮次
        scanned_case_override: 扫描的case编号（None表示自动推断）
        path_mapper: 路径映射器

    Returns:
        是否执行了复制（True表示任务在上一轮是STOPPED，已复制）
    """
    if iteration < 1:
        return False

    # 检查上一轮任务状态
    prev_task_info_path = path_mapper.get_iteration_task_info_path(task_id, iteration - 1)
    if not prev_task_info_path.exists():
        return False

    prev_task_info = TaskInfo.load_from_file(prev_task_info_path)

    if prev_task_info.status != TaskStatus.STOPPED:
        return False

    # 上一轮是STOPPED，执行复制
    _logger.info(f"Task {task_id}: Previous iteration was STOPPED, copying from iteration {iteration - 1}")

    _, _, _, case = path_mapper.parse_task_id(task_id)

    # 推断scanned_case（从上一轮）
    if scanned_case_override:
        scanned_case = scanned_case_override
    else:
        scanned_case = infer_scanned_case_for_task(task_id, iteration - 1, path_mapper)
        if scanned_case is None:
            _logger.warning(f"Task {task_id}: Failed to infer scanned_case from previous iteration")
            return False

    # 复制DSL文件
    prev_dsl_path = path_mapper.get_iteration_output_dir(task_id, iteration - 1) / f"{case}.kirin"
    current_dsl_path = path_mapper.get_iteration_output_dir(task_id, iteration) / f"{case}.kirin"
    if prev_dsl_path.exists():
        current_dsl_path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(prev_dsl_path, current_dsl_path)
        _logger.info(f"Task {task_id}: Copied DSL from iteration {iteration - 1}")

    # 复制检测结果
    prev_labeled_result_path = path_mapper.get_iteration_detect_result_path(
        task_id, iteration - 1, scanned_case
    )
    current_labeled_result_path = path_mapper.get_iteration_detect_result_path(
        task_id, iteration, scanned_case
    )
    if prev_labeled_result_path.exists():
        current_labeled_result_path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(prev_labeled_result_path, current_labeled_result_path)
        _logger.info(f"Task {task_id}: Copied detection results from iteration {iteration - 1}")

    # 复制并更新task_info（更新iteration和timestamp字段）
    current_task_info = prev_task_info
    current_task_info.iteration = iteration
    current_task_info.timestamp = get_current_timestamp()

    current_task_info_path = path_mapper.get_iteration_task_info_path(task_id, iteration)
    current_task_info_path.parent.mkdir(parents=True, exist_ok=True)
    current_task_info.save_to_file(current_task_info_path)
    _logger.info(f"Task {task_id}: Copied task_info from iteration {iteration - 1}")

    return True


def label_warnings(
    navi_warnings: List[Tuple[str, str, int]],
    ground_truth_warnings: List[dict],
    commit_methods: List[dict]
) -> List[dict]:
    """
    标注警告为TP/FP/FN

    Args:
        navi_warnings: Navi检测结果
        ground_truth_warnings: Ground truth警告
        commit_methods: Commit changed functions

    Returns:
        标注后的结果列表
    """
    labeled_results = []
    matched_gt_idx = set()

    def find_func_info(related_file_name, function_name, report_line):
        for func in commit_methods:
            func_file = func['file_name'].replace("/", "#")
            if (related_file_name in func_file and
                    function_name in func['method_signature'] and
                    int(func['begin_line']) - 1 <= int(report_line) <= int(func['end_line']) + 1):
                return func
        return None

    # 标注TP/FP
    for navi_warning in navi_warnings:
        navi_file_name, navi_function_name, navi_report_line = navi_warning
        is_tp = False
        matched_gt = None

        for j, gt in enumerate(ground_truth_warnings):
            gt_file = gt.get("file", "").replace("/", "#")
            if (navi_file_name in gt_file and
                    gt["begin_line"] - 1 <= navi_report_line <= gt["end_line"] + 1):
                is_tp = True
                matched_gt = gt
                matched_gt_idx.add(j)
                break

        func_info = find_func_info(navi_file_name, navi_function_name, navi_report_line)

        if func_info:
            labeled_results.append({
                "label": "tp" if is_tp else "fp",
                "file_name": func_info["file_name"],
                "begin_line": func_info["begin_line"],
                "end_line": func_info["end_line"],
                "method_signature": func_info["method_signature"],
                "method_source": func_info.get("method_source", ""),
                "navi_warning": {
                    "related_file_name": navi_file_name,
                    "function_name": navi_function_name,
                    "report_line": navi_report_line
                },
                "ground_truth": matched_gt if is_tp else None
            })

    # 标注FN
    for j, gt in enumerate(ground_truth_warnings):
        if j not in matched_gt_idx:
            gt_file = gt.get("file", "").replace("/", "#")
            for func in commit_methods:
                func_file = func["file_name"].replace("/", "#")
                if (gt_file in func_file and
                        int(func["begin_line"]) - 1 <= int(gt["begin_line"]) <= int(func["end_line"]) + 1):
                    labeled_results.append({
                        "label": "fn",
                        "file_name": func["file_name"],
                        "begin_line": func["begin_line"],
                        "end_line": func["end_line"],
                        "method_signature": func["method_signature"],
                        "method_source": func.get("method_source", ""),
                        "navi_warning": None,
                        "ground_truth": gt
                    })
                    break

    return labeled_results


def label_task(
    task_id: str,
    iteration: int,
    scanned_case_override: str,
    path_mapper: PathMapper
):
    """
    对单个任务进行标注

    Args:
        task_id: 任务ID
        iteration: 迭代轮次
        scanned_case_override: 扫描的case编号（None表示自动推断）
        path_mapper: 路径映射器
    """
    # 检查是否需要从上一轮复制（如果上一轮是STOPPED状态）
    if copy_from_previous_iteration(task_id, iteration, scanned_case_override, path_mapper):
        return  # 已复制，直接返回

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

    # 获取检测结果目录
    detect_results_dir = path_mapper.get_iteration_detect_results_dir(task_id, iteration)
    dataset, checker_name, group, case = path_mapper.parse_task_id(task_id)

    result_file = f"{case}_{scanned_case}"
    result_dir = detect_results_dir / result_file

    if not result_dir.exists():
        _logger.error(f"Task {task_id}: Detection results not found at {result_dir}")
        return

    # 查找XML文件
    xml_files = list(result_dir.glob("*.xml"))
    if not xml_files:
        _logger.error(f"Task {task_id}: No XML files found in {result_dir}")
        return

    xml_file = xml_files[0]
    _logger.info(f"Task {task_id}: Processing {xml_file}")

    # 解析Navi警告
    navi_warnings = parse_navi_warnings(xml_file)
    _logger.info(f"Task {task_id}: Found {len(navi_warnings)} Navi warnings")

    # 加载ground truth和commit methods
    commit_path = path_mapper.config.commit_root / dataset / checker_name / group / scanned_case
    sat_warnings_path = commit_path / "sat_warnings.json"
    methods_path = commit_path / "methods.json"

    ground_truth_warnings = load_ground_truth_warnings(sat_warnings_path)
    commit_methods = load_commit_methods(methods_path)

    _logger.info(f"Task {task_id}: Loaded {len(ground_truth_warnings)} ground truth warnings")
    _logger.info(f"Task {task_id}: Loaded {len(commit_methods)} commit methods")

    # 标注警告
    labeled_results = label_warnings(navi_warnings, ground_truth_warnings, commit_methods)

    # 统计TP/FP/FN
    tp_count = sum(1 for r in labeled_results if r["label"] == "tp")
    fp_count = sum(1 for r in labeled_results if r["label"] == "fp")
    fn_count = sum(1 for r in labeled_results if r["label"] == "fn")

    _logger.info(f"Task {task_id}: TP={tp_count}, FP={fp_count}, FN={fn_count}")

    # 保存标注结果
    labeled_result_path = path_mapper.get_iteration_detect_result_path(task_id, iteration, scanned_case)
    labeled_result_path.parent.mkdir(parents=True, exist_ok=True)

    with open(labeled_result_path, 'w', encoding='utf-8') as f:
        json.dump(labeled_results, f, indent=2, ensure_ascii=False)

    _logger.info(f"Task {task_id}: Saved labeled results to {labeled_result_path}")

    # 计算新的detection_metrics
    new_detection_metrics = DetectionMetrics.from_counts(tp=tp_count, fp=fp_count, fn=fn_count)

    # 检查指标是否退化（仅对iteration >= 1进行检查）
    if iteration >= 1:
        # 加载上一轮的task_info来比较指标
        prev_task_info_path = path_mapper.get_iteration_task_info_path(task_id, iteration - 1)
        if prev_task_info_path.exists():
            prev_task_info = TaskInfo.load_from_file(prev_task_info_path)
            prev_metrics = prev_task_info.metrics.detection_metrics

            if prev_metrics:
                # 比较F1分数
                prev_f1 = prev_metrics.f1_score
                new_f1 = new_detection_metrics.f1_score

                if new_f1 < prev_f1:
                    # 指标退化，执行回滚
                    _logger.warning(f"Task {task_id}: Metrics degraded! F1: {prev_f1:.4f} → {new_f1:.4f}")
                    _logger.info(f"Task {task_id}: Rolling back to previous iteration...")

                    # 回滚DSL文件
                    current_dsl_path = path_mapper.get_iteration_output_dir(task_id, iteration) / f"{case}.kirin"
                    prev_dsl_path = path_mapper.get_iteration_output_dir(task_id, iteration - 1) / f"{case}.kirin"
                    if prev_dsl_path.exists():
                        shutil.copy2(prev_dsl_path, current_dsl_path)
                        _logger.info(f"Task {task_id}: Rolled back DSL from iteration {iteration - 1}")

                    # 回滚检测结果
                    prev_labeled_result_path = path_mapper.get_iteration_detect_result_path(
                        task_id, iteration - 1, scanned_case
                    )
                    if prev_labeled_result_path.exists():
                        shutil.copy2(prev_labeled_result_path, labeled_result_path)
                        _logger.info(f"Task {task_id}: Rolled back detection results from iteration {iteration - 1}")

                    # 使用上一轮的指标
                    new_detection_metrics = prev_metrics

                    # 标记任务为stopped，原因是metrics_degraded
                    task_info.status = TaskStatus.STOPPED
                    task_info.stop_reason = StopReason.METRICS_DEGRADED
                    _logger.info(f"Task {task_id}: Marked as STOPPED (METRICS_DEGRADED)")

    # 更新task_info.json的detection_metrics
    task_info.metrics.detection_metrics = new_detection_metrics

    task_info.save_to_file(task_info_path)
    _logger.info(f"Task {task_id}: Updated task_info.json with detection metrics")


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description="Label detection results and update metrics")
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
        help="Override scanned case number (default: auto-infer from detect results). Use '3' for RQ4 validation."
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=None,
        help="Limit the number of tasks to process (for testing)"
    )

    args = parser.parse_args()

    iteration = args.iteration
    scanned_case_override = args.scanned_case
    limit = args.limit

    _logger.info("=" * 80)
    _logger.info(f"Batch Labeling Runner - Iteration {iteration}")
    _logger.info("=" * 80)

    # 加载配置
    config = get_default_config()
    path_mapper = PathMapper(config)

    # 扫描任务
    task_ids = path_mapper.scan_dsl_files()
    _logger.info(f"Found {len(task_ids)} DSL tasks")

    if limit:
        task_ids = task_ids[:limit]
        _logger.info(f"Limited to {len(task_ids)} tasks")

    # 对每个任务进行标注
    for i, task_id in enumerate(task_ids, 1):
        _logger.info(f"[{i}/{len(task_ids)}] Processing task: {task_id}")
        label_task(task_id, iteration, scanned_case_override, path_mapper)

    _logger.info("=" * 80)
    _logger.info("Labeling completed!")
    _logger.info("Next step: Run next iteration")
    _logger.info(f"  python batch_runner.py --iteration {iteration + 1}")


if __name__ == "__main__":
    main()
