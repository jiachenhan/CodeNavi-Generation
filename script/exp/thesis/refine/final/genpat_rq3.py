"""
GenPat RQ3实验 - Baseline对比

使用GenPat工具运行RQ3实验，作为baseline与Kirin DSL结果对比。

使用方法:
    python -m exp.thesis.refine.final.genpat_rq3 \
        --genpat-jar /path/to/GenPat.jar
"""
import argparse
import json
from pathlib import Path
from typing import List, Optional, Dict, Tuple, Callable

from exp.thesis.refine.experiment_config import ExperimentConfig
from exp.thesis.refine.final.run_rq3 import load_case_selections, scan_dsl_files, RQ3TaskResult, RQ3Summary
from exp.thesis.refine.data_structures import DetectionMetrics
from interface.java.run_java_api import genpat_detect_all
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


# ============================================================================
# GenPat结果解析
# ============================================================================

def collect_genpat_warnings(result_file: Path) -> List[Tuple[str, str, str, str, str, str]]:
    """
    收集GenPat检测结果

    Args:
        result_file: GenPat结果文件路径 (result.txt)

    Returns:
        List of (file_path, sig_name, ret_type, params, begin_line, end_line)
    """
    if not result_file.exists():
        _logger.error(f"GenPat result file not found: {result_file}")
        return []

    lines = open(result_file, 'r', encoding="ISO-8859-1").readlines()
    ret_warnings = []

    for line in lines:
        line = line.strip()
        if len(line) == 0 or line.startswith("Empty Pattern"):
            continue

        try:
            _, file_info, sig_info, line_info = line.split("\t")

            file_path = file_info.replace("\\", "/").split("/")[-1]

            sig_name = sig_info.split(" ")[1].split("[")[0].strip()
            ret_type = sig_info.split(" ")[0].strip()
            params = sig_info.split("[")[1].split("]")[0].strip()
            begin_line = line_info.split("#")[0].strip()
            end_line = line_info.split("#")[1].strip()

            ret_warnings.append((file_path, sig_name, ret_type, params, begin_line, end_line))
        except Exception as e:
            _logger.warning(f"Failed to parse GenPat warning line: {line[:100]}... Error: {e}")
            continue

    return ret_warnings


def filter_genpat_warnings(
    genpat_warnings: List[Tuple],
    changed_methods: List[dict]
) -> List[Tuple]:
    """
    过滤GenPat warnings，只保留changed methods中的

    Args:
        genpat_warnings: GenPat检测结果
        changed_methods: 变更方法列表

    Returns:
        过滤后的warnings
    """
    result = []
    for genpat_warning in genpat_warnings:
        file_path, sig_name, ret_type, param_types, begin_line, end_line = genpat_warning
        for changed_method in changed_methods:
            if (file_path == changed_method["file_name"].replace("/", "#")
                    and begin_line == str(changed_method["begin_line"])
                    and end_line == str(changed_method["end_line"])):
                result.append(genpat_warning)
                break
    return result


def calculate_metrics(
    ground_truth_warnings: List[dict],
    genpat_warnings: List[Tuple],
    all_changed_methods_num: int,
    match_func: Callable[[dict, Tuple], bool]
) -> Tuple[int, int, int, int, float, float, float, float]:
    """
    计算检测指标

    Args:
        ground_truth_warnings: Ground truth列表
        genpat_warnings: GenPat检测结果
        all_changed_methods_num: 所有变更方法数量
        match_func: 匹配函数

    Returns:
        (tp, tn, fp, fn, accuracy, precision, recall, f1_score)
    """
    matched = [False] * len(ground_truth_warnings)
    tp = 0
    fp = 0

    for genpat_warning in genpat_warnings:
        match_idx = next((i for i, gt in enumerate(ground_truth_warnings) if not matched[i]
                          and match_func(gt, genpat_warning)), None)
        if match_idx is not None:
            matched[match_idx] = True
            tp += 1
        else:
            fp += 1

    fn = len(ground_truth_warnings) - sum(matched)
    tn = all_changed_methods_num - tp - fp - fn

    accuracy = (tp + tn) / (tp + fp + fn + tn) if (tp + fp + fn + tn) > 0 else 0
    precision = tp / (tp + fp) if (tp + fp) > 0 else 0
    recall = tp / (tp + fn) if (tp + fn) > 0 else 0
    f1_score = (2 * precision * recall / (precision + recall)) if (precision + recall) > 0 else 0

    return tp, tn, fp, fn, accuracy, precision, recall, f1_score


# ============================================================================
# RQ3实验主逻辑
# ============================================================================

def run_genpat_on_test_case(
    task_id: str,
    case1: str,
    case2: str,
    test_case: str,
    config: ExperimentConfig,
    genpat_jar: Path
) -> RQ3TaskResult:
    """
    使用GenPat在测试case上运行检测

    Args:
        task_id: 任务ID
        case1: case1
        case2: case2
        test_case: 测试case
        config: 实验配置
        genpat_jar: GenPat JAR路径

    Returns:
        RQ3TaskResult对象
    """
    dataset, checker, group, _ = task_id.split('/')
    dsl_file = f"{case1}_{case2} (GenPat)"

    _logger.info(f"Task {task_id}: Running GenPat on case {test_case}")

    # 检查commit路径
    test_commit_path = config.commit_root / dataset / checker / group / test_case / "after"
    if not test_commit_path.exists():
        error_msg = f"Test commit path not found: {test_commit_path}"
        _logger.error(f"Task {task_id}: {error_msg}")
        return RQ3TaskResult(
            task_id=task_id,
            dsl_file=dsl_file,
            case1=case1,
            case2=case2,
            test_case=test_case,
            detection_metrics=None,
            labeled_result_path=None,
            status="failed",
            error_message=error_msg
        )

    # 获取pattern路径 (case1的before和after)
    case1_dir = config.commit_root / dataset / checker / group / case1
    pattern_before_path = case1_dir / "before" / "buggy.java"
    pattern_after_path = case1_dir / "after" / "fixed.java"

    if not pattern_before_path.exists() or not pattern_after_path.exists():
        error_msg = f"Pattern files not found: before={pattern_before_path}, after={pattern_after_path}"
        _logger.error(f"Task {task_id}: {error_msg}")
        return RQ3TaskResult(
            task_id=task_id,
            dsl_file=dsl_file,
            case1=case1,
            case2=case2,
            test_case=test_case,
            detection_metrics=None,
            labeled_result_path=None,
            status="failed",
            error_message=error_msg
        )

    # 创建输出目录
    task_output_dir = config.rq3_exp_root / "genpat_version" / task_id
    task_output_dir.mkdir(parents=True, exist_ok=True)

    result_file = task_output_dir / "result.txt"

    # 运行GenPat检测
    _logger.info(f"Task {task_id}: Running GenPat detection...")
    _logger.info(f"  Pattern before: {pattern_before_path}")
    _logger.info(f"  Pattern after: {pattern_after_path}")
    _logger.info(f"  Scanned: {test_commit_path}")
    _logger.info(f"  Output: {result_file}")

    timeout = genpat_detect_all(
        30 * 60,  # 30分钟超时
        pattern_before_path,
        pattern_after_path,
        test_commit_path,
        result_file,
        genpat_jar
    )

    if timeout:
        _logger.error(f"Task {task_id}: GenPat detection timeout")
        return RQ3TaskResult(
            task_id=task_id,
            dsl_file=dsl_file,
            case1=case1,
            case2=case2,
            test_case=test_case,
            detection_metrics=None,
            labeled_result_path=None,
            status="failed",
            error_message="GenPat detection timeout"
        )

    # 加载ground truth
    ground_truth_path = test_commit_path.parent / "sat_warnings.json"
    if not ground_truth_path.exists():
        _logger.warning(f"Task {task_id}: Ground truth not found: {ground_truth_path}")
        ground_truth_warnings = []
    else:
        with open(ground_truth_path, 'r', encoding='utf-8') as f:
            try:
                ground_truth_warnings = json.load(f)
            except:
                ground_truth_warnings = []

    # 加载changed methods
    methods_path = test_commit_path.parent / "methods.json"
    if not methods_path.exists():
        _logger.warning(f"Task {task_id}: methods.json not found: {methods_path}")
        changed_methods = []
    else:
        with open(methods_path, 'r', encoding='utf-8') as f:
            changed_methods = json.load(f)

    # 收集GenPat warnings
    genpat_warnings = collect_genpat_warnings(result_file)
    genpat_warnings = filter_genpat_warnings(genpat_warnings, changed_methods)

    # 定义匹配函数
    def matched_warnings(ground_truth_warning: dict, genpat_warning: Tuple) -> bool:
        truth_path = ground_truth_warning["file"]
        truth_begin_line = str(ground_truth_warning["begin_line"])
        truth_end_line = str(ground_truth_warning["end_line"])
        file_path, genpat_func_name, return_type, param_types, begin_line, end_line = genpat_warning
        return (truth_path == file_path and truth_begin_line == begin_line
                and truth_end_line == end_line)

    # 计算指标
    tp, tn, fp, fn, accuracy, precision, recall, f1_score = calculate_metrics(
        ground_truth_warnings,
        genpat_warnings,
        len(changed_methods),
        matched_warnings
    )

    _logger.info(f"Task {task_id}: TP={tp}, FP={fp}, FN={fn}")

    # 保存标注结果
    labeled_results = []

    # 标注TP
    matched = [False] * len(ground_truth_warnings)
    for genpat_warning in genpat_warnings:
        match_idx = next((i for i, gt in enumerate(ground_truth_warnings) if not matched[i]
                          and matched_warnings(gt, genpat_warning)), None)
        if match_idx is not None:
            matched[match_idx] = True
            labeled_results.append({
                "file": genpat_warning[0],
                "begin_line": int(genpat_warning[4]),
                "end_line": int(genpat_warning[5]),
                "label": "tp"
            })
        else:
            labeled_results.append({
                "file": genpat_warning[0],
                "begin_line": int(genpat_warning[4]),
                "end_line": int(genpat_warning[5]),
                "label": "fp"
            })

    # 标注FN
    for i, gt in enumerate(ground_truth_warnings):
        if not matched[i]:
            labeled_results.append({
                "file": gt["file"],
                "begin_line": gt["begin_line"],
                "end_line": gt["end_line"],
                "label": "fn"
            })

    labeled_result_path = task_output_dir / "labeled_results.json"
    with open(labeled_result_path, 'w', encoding='utf-8') as f:
        json.dump(labeled_results, f, indent=2, ensure_ascii=False)

    _logger.info(f"Task {task_id}: Saved labeled results to {labeled_result_path}")

    # 计算检测指标
    detection_metrics = DetectionMetrics.from_counts(tp=tp, fp=fp, fn=fn)

    # 返回相对路径
    relative_labeled_result_path = str(labeled_result_path.relative_to(config.rq3_exp_root))

    return RQ3TaskResult(
        task_id=task_id,
        dsl_file=dsl_file,
        case1=case1,
        case2=case2,
        test_case=test_case,
        detection_metrics=detection_metrics,
        labeled_result_path=relative_labeled_result_path,
        status="success",
        error_message=None
    )


def genpat_rq3_exp(
    config: ExperimentConfig,
    genpat_jar: Path,
    case_selections: Dict[str, Dict[str, str]]
) -> RQ3Summary:
    """
    运行GenPat RQ3实验

    Args:
        config: 实验配置
        genpat_jar: GenPat JAR路径
        case_selections: 预先生成的case选择字典

    Returns:
        RQ3Summary对象
    """
    # 扫描所有DSL文件（使用final_dsl_root作为任务列表）
    dsl_files = scan_dsl_files(config.final_dsl_root)

    # 处理每个任务
    all_results = []
    tested_count = 0
    skipped_count = 0
    failed_count = 0

    for i, (task_id, dsl_path) in enumerate(dsl_files, 1):
        _logger.info(f"\n[{i}/{len(dsl_files)}] Processing task: {task_id}")

        # 从文件名提取case1和case2
        dsl_filename = dsl_path.name
        if not dsl_filename.endswith('.kirin'):
            _logger.warning(f"Task {task_id}: Invalid DSL filename: {dsl_filename}")
            continue

        base_name = dsl_path.stem
        parts = base_name.split('_')
        if len(parts) != 2:
            _logger.warning(f"Task {task_id}: Invalid DSL filename format: {dsl_filename}")
            continue

        case1, case2 = parts

        # 从预先生成的选择中获取test_case
        if task_id not in case_selections:
            _logger.warning(f"Task {task_id}: No test case selection available, skipping")
            result = RQ3TaskResult(
                task_id=task_id,
                dsl_file=f"{case1}_{case2} (GenPat)",
                case1=case1,
                case2=case2,
                test_case=None,
                detection_metrics=None,
                labeled_result_path=None,
                status="skipped",
                error_message="No test case selection available"
            )
            all_results.append(result)
            skipped_count += 1
            continue

        selection = case_selections[task_id]
        test_case = selection["case3"]
        _logger.info(f"Task {task_id}: Using pre-selected test case: {test_case}")

        # 运行GenPat检测
        result = run_genpat_on_test_case(
            task_id,
            case1,
            case2,
            test_case,
            config,
            genpat_jar
        )

        all_results.append(result)

        if result.status == "success":
            tested_count += 1
        elif result.status == "failed":
            failed_count += 1

    # 生成汇总报告
    _logger.info("\n" + "=" * 80)
    _logger.info("Generating GenPat summary report...")
    _logger.info("=" * 80)

    # 计算平均指标（只考虑成功的测试）
    successful_results = [r for r in all_results if r.status == "success" and r.detection_metrics]

    if successful_results:
        avg_precision = sum(r.detection_metrics.precision for r in successful_results) / len(successful_results)
        avg_recall = sum(r.detection_metrics.recall for r in successful_results) / len(successful_results)
        avg_f1 = sum(r.detection_metrics.f1_score for r in successful_results) / len(successful_results)
        avg_fp_count = sum(r.detection_metrics.fp_count for r in successful_results) / len(successful_results)
    else:
        avg_precision = avg_recall = avg_f1 = avg_fp_count = 0.0

    summary = RQ3Summary(
        total_tasks=len(dsl_files),
        tested_tasks=tested_count,
        skipped_tasks=skipped_count,
        failed_tasks=failed_count,
        task_results=all_results,
        avg_precision=avg_precision,
        avg_recall=avg_recall,
        avg_f1=avg_f1,
        avg_fp_count=avg_fp_count
    )

    # 保存汇总报告
    summary_path = config.rq3_exp_root / "genpat_version" / "rq3_summary.json"
    summary_path.parent.mkdir(parents=True, exist_ok=True)
    summary.save_to_file(summary_path)

    # 打印统计信息
    _logger.info("\n" + "=" * 80)
    _logger.info("GenPat RQ3实验完成!")
    _logger.info("=" * 80)
    _logger.info(f"总任务数: {len(dsl_files)}")
    _logger.info(f"成功测试: {tested_count}")
    _logger.info(f"跳过: {skipped_count} (无额外测试case)")
    _logger.info(f"失败: {failed_count}")
    _logger.info("")
    _logger.info("平均指标（基于成功测试）:")
    _logger.info(f"  Precision: {avg_precision:.4f}")
    _logger.info(f"  Recall: {avg_recall:.4f}")
    _logger.info(f"  F1: {avg_f1:.4f}")
    _logger.info(f"  平均FP数量: {avg_fp_count:.2f}")
    _logger.info("")
    _logger.info(f"汇总报告: {summary_path}")
    _logger.info("=" * 80)

    return summary


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description="GenPat RQ3实验：测试GenPat的泛化能力")
    parser.add_argument(
        "--genpat-jar",
        type=str,
        required=True,
        help="GenPat JAR路径"
    )

    args = parser.parse_args()
    genpat_jar = Path(args.genpat_jar)

    if not genpat_jar.exists():
        _logger.error(f"GenPat JAR不存在: {genpat_jar}")
        return 1

    _logger.info("=" * 80)
    _logger.info("GenPat RQ3实验：测试GenPat的泛化能力")
    _logger.info("=" * 80)

    # 初始化配置
    config = ExperimentConfig()

    _logger.info(f"Commit root: {config.commit_root}")
    _logger.info(f"GenPat JAR: {genpat_jar}")
    _logger.info("")

    # 加载共享的case选择（与Kirin实验使用相同的测试case）
    _logger.info("Loading test case selections...")
    case_selections = load_case_selections(config)

    if case_selections is None:
        _logger.error("Case selections not found! Please run Kirin RQ3 experiment first to generate case selections.")
        return 1

    _logger.info(f"Loaded {len(case_selections)} test case selections")
    _logger.info("")

    # 运行GenPat实验
    _logger.info("Running GenPat RQ3 experiment...")
    genpat_rq3_exp(config, genpat_jar, case_selections)

    return 0


if __name__ == '__main__':
    exit(main())
