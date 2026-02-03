"""
RQ3实验脚本：测试最终版本DSL的泛化能力

使用final_version目录下的DSL文件在其他case上进行检测，验证泛化能力。

DSL文件命名格式: {case1}_{case2}.kirin
测试: 使用除了case1和case2之外的其他case

使用方法:
    python -m exp.thesis.refine.final.run_rq3 \
        --engine-path D:/envs/kirin-cli-1.0.8_sp06-jackofext-obfuscate.jar
"""
import argparse
import json
import random
from pathlib import Path
from typing import List, Optional, Tuple, Dict
from dataclasses import dataclass
from bs4 import BeautifulSoup

from exp.thesis.refine.experiment_config import ExperimentConfig
from exp.thesis.refine.data_structures import DetectionMetrics
from interface.java.run_java_api import kirin_engine
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


# ============================================================================
# 配置常量
# ============================================================================

# 统计时忽略的checker列表
IGNORED_CHECKERS = [
    # CodeQL checkers
    "Expression_always_evaluates_to_the_same_value",
    "Missing_Override_annotation",
    "Deprecated_method_or_constructor_invocation",
    # PMD checkers
    "AssignmentInOperand",
    "JUnit5TestShouldBePackagePrivate",
    "NullAssignment"
]


# ============================================================================
# 数据结构
# ============================================================================

@dataclass
class RQ3TaskResult:
    """单个任务的RQ3测试结果"""
    task_id: str                     # 任务ID
    dsl_file: str                    # DSL文件名（{case1}_{case2}.kirin）
    case1: str                       # case1
    case2: str                       # case2
    test_case: Optional[str]         # 测试case
    detection_metrics: Optional[DetectionMetrics]  # 检测指标
    labeled_result_path: Optional[str]  # 标注结果路径（相对于final_version）
    status: str                      # 状态: success, skipped
    error_message: Optional[str]     # 错误信息（如果失败）

    def to_dict(self):
        return {
            "task_id": self.task_id,
            "dsl_file": self.dsl_file,
            "case1": self.case1,
            "case2": self.case2,
            "test_case": self.test_case,
            "detection_metrics": self.detection_metrics.to_dict() if self.detection_metrics else None,
            "labeled_result_path": self.labeled_result_path,
            "status": self.status,
            "error_message": self.error_message
        }


@dataclass
class RQ3Summary:
    """RQ3实验汇总"""
    total_tasks: int                 # 总任务数（DSL文件数）
    tested_tasks: int                # 成功测试的任务数
    skipped_tasks: int               # 跳过的任务数（无额外case）
    failed_tasks: int                # 失败的任务数
    task_results: List[RQ3TaskResult]  # 任务结果列表
    avg_precision: float
    avg_recall: float
    avg_f1: float
    avg_fp_count: float

    def to_dict(self):
        return {
            "total_tasks": self.total_tasks,
            "tested_tasks": self.tested_tasks,
            "skipped_tasks": self.skipped_tasks,
            "failed_tasks": self.failed_tasks,
            "avg_precision": self.avg_precision,
            "avg_recall": self.avg_recall,
            "avg_f1": self.avg_f1,
            "avg_fp_count": self.avg_fp_count,
            "task_results": [r.to_dict() for r in self.task_results]
        }

    def save_to_file(self, path: Path):
        """保存到JSON文件"""
        with open(path, 'w', encoding='utf-8') as f:
            json.dump(self.to_dict(), f, indent=2, ensure_ascii=False)
        _logger.info(f"Saved RQ3 summary to {path}")


# ============================================================================
# 辅助函数
# ============================================================================

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
    """加载ground truth警告"""
    if not sat_warnings_path.exists():
        _logger.warning(f"Ground truth warnings not found: {sat_warnings_path}")
        return []

    with open(sat_warnings_path, 'r', encoding='utf-8') as f:
        warnings = json.load(f)

    return warnings


def load_commit_methods(methods_path: Path) -> List[dict]:
    """加载commit changed functions信息"""
    if not methods_path.exists():
        _logger.warning(f"Methods info not found: {methods_path}")
        return []

    with open(methods_path, 'r', encoding='utf-8') as f:
        methods = json.load(f)

    return methods


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


def get_available_test_cases(
    commit_root: Path,
    dataset: str,
    checker: str,
    group: str,
    case1: str,
    case2: str
) -> List[str]:
    """
    获取可用的测试cases（除了训练case1和case2之外的）

    Args:
        commit_root: commit根目录
        dataset: 数据集名称
        checker: Checker名称
        group: 组号
        case1: case1
        case2: case2

    Returns:
        测试case列表
    """
    group_dir = commit_root / dataset / checker / group
    if not group_dir.exists():
        _logger.warning(f"Group directory not found: {group_dir}")
        return []

    # 列出所有case目录（假设是数字命名）
    all_cases = []
    for case_dir in group_dir.iterdir():
        if case_dir.is_dir() and case_dir.name.isdigit():
            all_cases.append(case_dir.name)

    # 过滤掉训练cases
    test_cases = [c for c in all_cases if c not in [case1, case2]]

    return sorted(test_cases)


def run_detection_on_test_case(
    dsl_path: Path,
    task_id: str,
    case1: str,
    case2: str,
    test_case: str,
    config: ExperimentConfig,
    engine_path: Path,
    mode_version: str  # "final_version" 或 "origin_version"
) -> RQ3TaskResult:
    """
    在测试case上运行检测

    Args:
        dsl_path: DSL文件路径
        task_id: 任务ID
        case1: case1
        case2: case2
        test_case: 测试case
        config: 实验配置
        engine_path: Kirin引擎路径
        mode_version: 实验版本 ("final_version" 或 "origin_version")

    Returns:
        RQ3TaskResult对象
    """
    dataset, checker, group, _ = task_id.split('/')
    dsl_file = f"{case1}_{case2}.kirin"

    _logger.info(f"Task {task_id}: Testing DSL on case {test_case}")

    # 获取测试case的commit路径
    commit_path = config.commit_root / dataset / checker / group / test_case / "after"
    if not commit_path.exists():
        error_msg = f"Commit path not found: {commit_path}"
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

    # 创建输出目录（在rq3_exp/{mode_version}/{task_id}/下）
    task_output_dir = config.rq3_exp_root / mode_version / task_id
    task_output_dir.mkdir(parents=True, exist_ok=True)

    detect_results_dir = task_output_dir / "detect_results"
    detect_results_dir.mkdir(parents=True, exist_ok=True)

    result_path = detect_results_dir

    # 运行检测
    _logger.info(f"Task {task_id}: Running detection...")
    _logger.info(f"  DSL: {dsl_path}")
    _logger.info(f"  Commit: {commit_path}")
    _logger.info(f"  Output: {result_path}")

    timeout = kirin_engine(300, engine_path, dsl_path, commit_path, result_path)

    if timeout:
        error_msg = "Detection timeout"
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

    # 查找XML文件
    xml_files = list(result_path.glob("*.xml"))
    if not xml_files:
        error_msg = "No XML files found"
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

    xml_file = xml_files[0]
    _logger.info(f"Task {task_id}: Processing {xml_file}")

    # 解析Navi警告
    navi_warnings = parse_navi_warnings(xml_file)
    _logger.info(f"Task {task_id}: Found {len(navi_warnings)} Navi warnings")

    # 加载ground truth和commit methods
    test_commit_path = config.commit_root / dataset / checker / group / test_case
    sat_warnings_path = test_commit_path / "sat_warnings.json"
    methods_path = test_commit_path / "methods.json"

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
    labeled_result_path = task_output_dir / "labeled_results.json"
    with open(labeled_result_path, 'w', encoding='utf-8') as f:
        json.dump(labeled_results, f, indent=2, ensure_ascii=False)

    _logger.info(f"Task {task_id}: Saved labeled results to {labeled_result_path}")

    # 计算检测指标
    detection_metrics = DetectionMetrics.from_counts(tp=tp_count, fp=fp_count, fn=fn_count)

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


def scan_dsl_files(dsl_root: Path) -> List[Tuple[str, Path]]:
    """
    扫描所有DSL文件

    支持两种目录结构:
    - Final格式: {dataset}/{checker}/{group}/{case}/{case1}_{case2}.kirin
    - Origin格式: {dataset}/{checker}/{group}/{case}.kirin

    Args:
        dsl_root: DSL根目录

    Returns:
        [(task_id, dsl_path), ...]
    """
    dsl_files = []
    invalid_files = []
    format_counts = {"final": 0, "origin": 0}

    for dsl_path in dsl_root.rglob("*.kirin"):
        # 获取相对路径
        relative_path = dsl_path.relative_to(dsl_root)
        parts = relative_path.parts

        # 判断是final还是origin格式
        if len(parts) == 5:
            # Final格式: dataset/checker/group/case/filename.kirin
            # task_id不包含文件名
            task_id = "/".join(parts[:-1])
            format_counts["final"] += 1
        elif len(parts) == 4:
            # Origin格式: dataset/checker/group/filename.kirin
            # case从文件名中提取 (例如 "1.kirin" -> "1")
            dataset, checker, group, _ = parts
            case = dsl_path.stem  # 去掉.kirin后缀
            task_id = f"{dataset}/{checker}/{group}/{case}"
            format_counts["origin"] += 1
        else:
            _logger.warning(f"Invalid path structure (expected 4 or 5 parts): {relative_path}")
            invalid_files.append(str(relative_path))
            continue

        dsl_files.append((task_id, dsl_path))

    _logger.info(f"Found {len(dsl_files)} DSL files in {dsl_root}")
    _logger.info(f"  - Final格式: {format_counts['final']}")
    _logger.info(f"  - Origin格式: {format_counts['origin']}")
    if invalid_files:
        _logger.warning(f"  - 无效文件: {len(invalid_files)}")
        for invalid in invalid_files:
            _logger.warning(f"      {invalid}")

    return sorted(dsl_files)


# ============================================================================
# Case选择生成和加载
# ============================================================================

def load_case_selections(config: ExperimentConfig) -> Optional[Dict[str, Dict[str, str]]]:
    """
    加载已保存的case选择

    Args:
        config: 实验配置

    Returns:
        如果文件存在，返回 Dict[task_id, {"case1": str, "case2": str, "case3": str}]
        否则返回 None
    """
    case_selection_path = config.rq3_exp_root / "case_selection.json"

    if not case_selection_path.exists():
        return None

    _logger.info(f"Loading case selections from {case_selection_path}")
    with open(case_selection_path, 'r', encoding='utf-8') as f:
        selections_list = json.load(f)

    # 转换为字典格式
    selections = {item["task_id"]: item for item in selections_list}
    _logger.info(f"Loaded {len(selections)} case selections")
    return selections


def generate_case_selections(
    dsl_root: Path,
    commit_root: Path,
    config: ExperimentConfig
) -> Dict[str, Dict[str, str]]:
    """
    生成新的case选择并保存

    Args:
        dsl_root: DSL根目录
        commit_root: commit根目录
        config: 实验配置

    Returns:
        Dict[task_id, {"case1": str, "case2": str, "case3": str}]
    """
    _logger.info("Generating new case selections...")
    dsl_files = scan_dsl_files(dsl_root)
    selections = {}

    for task_id, dsl_path in dsl_files:
        # 从文件名提取case1和case2
        if not dsl_path.name.endswith('.kirin'):
            continue

        base_name = dsl_path.stem
        parts = base_name.split('_')
        if len(parts) != 2:
            continue

        case1, case2 = parts
        dataset, checker, group, _ = task_id.split('/')

        # 获取可用的测试cases
        test_cases = get_available_test_cases(
            commit_root,
            dataset,
            checker,
            group,
            case1,
            case2
        )

        if test_cases:
            # 随机选择一个测试case
            test_case = random.choice(test_cases)
            selections[task_id] = {
                "task_id": task_id,
                "case1": case1,
                "case2": case2,
                "case3": test_case
            }
            _logger.info(f"Task {task_id}: Selected case3={test_case} from {test_cases}")

    # 保存到文件
    case_selection_path = config.rq3_exp_root / "case_selection.json"
    case_selection_path.parent.mkdir(parents=True, exist_ok=True)
    selections_list = list(selections.values())
    with open(case_selection_path, 'w', encoding='utf-8') as f:
        json.dump(selections_list, f, indent=2, ensure_ascii=False)

    _logger.info(f"Saved case selections to {case_selection_path}")
    _logger.info(f"Generated {len(selections)} case selections")

    return selections


def generate_or_load_case_selections(
    dsl_root: Path,
    commit_root: Path,
    config: ExperimentConfig
) -> Dict[str, Dict[str, str]]:
    """
    生成或加载case选择（便捷函数）

    如果case_selection.json已存在，则加载；否则生成新的选择并保存。
    这样可以确保origin和final使用相同的测试case。

    Args:
        dsl_root: DSL根目录
        commit_root: commit根目录
        config: 实验配置

    Returns:
        Dict[task_id, {"case1": str, "case2": str, "case3": str}]
    """
    # 尝试加载已有选择
    selections = load_case_selections(config)
    if selections is not None:
        return selections

    # 如果不存在，生成新的选择
    return generate_case_selections(dsl_root, commit_root, config)


# ============================================================================
# 汇总统计函数
# ============================================================================

def generate_summary_from_results(
    dsl_root: Path,
    config: ExperimentConfig,
    mode_version: str,
    case_selections: Dict[str, Dict[str, str]]
) -> RQ3Summary:
    """
    从已有的检测结果生成汇总报告(不运行检测)

    Args:
        dsl_root: DSL根目录
        config: 实验配置
        mode_version: "final_version" 或 "origin_version"
        case_selections: case选择字典

    Returns:
        RQ3Summary对象
    """
    _logger.info(f"Generating summary from existing results ({mode_version})...")

    # 扫描DSL文件
    dsl_files = scan_dsl_files(dsl_root)

    all_results = []
    tested_count = 0
    skipped_count = 0
    failed_count = 0

    # 分类收集任务信息（用于详细日志）
    tested_tasks = []
    ignored_checker_tasks = []
    no_results_tasks = []
    failed_tasks = []

    for i, (task_id, dsl_path) in enumerate(dsl_files, 1):
        _logger.info(f"[{i}/{len(dsl_files)}] Processing task: {task_id}")

        # 检查是否在忽略列表中
        _, checker, _, _ = task_id.split('/')
        if checker in IGNORED_CHECKERS:
            _logger.info(f"Task {task_id}: Checker in ignored list, skipping")
            ignored_checker_tasks.append(task_id)
            skipped_count += 1
            continue

        # 检查labeled_results.json是否存在
        task_output_dir = config.rq3_exp_root / mode_version / task_id
        labeled_result_path = task_output_dir / "labeled_results.json"

        if not labeled_result_path.exists():
            _logger.warning(f"Task {task_id}: No results found, skipping")
            no_results_tasks.append(task_id)
            skipped_count += 1
            continue

        # 加载检测结果
        try:
            with open(labeled_result_path, 'r', encoding='utf-8') as f:
                labeled_results = json.load(f)
        except Exception as e:
            _logger.error(f"Task {task_id}: Failed to load results: {e}")
            failed_tasks.append(task_id)
            failed_count += 1
            continue

        # 统计TP/FP/FN
        tp_count = sum(1 for r in labeled_results if r["label"] == "tp")
        fp_count = sum(1 for r in labeled_results if r["label"] == "fp")
        fn_count = sum(1 for r in labeled_results if r["label"] == "fn")

        # 计算指标
        detection_metrics = DetectionMetrics.from_counts(tp=tp_count, fp=fp_count, fn=fn_count)

        # 从case_selections获取case信息
        if task_id in case_selections:
            selection = case_selections[task_id]
            case1 = selection["case1"]
            case2 = selection["case2"]
            test_case = selection["case3"]
        else:
            # Fallback: 尝试从文件名推断
            case1 = case2 = test_case = None

        # 创建结果
        result = RQ3TaskResult(
            task_id=task_id,
            dsl_file=dsl_path.name,
            case1=case1,
            case2=case2,
            test_case=test_case,
            detection_metrics=detection_metrics,
            labeled_result_path=str(labeled_result_path.relative_to(config.rq3_exp_root)),
            status="success",
            error_message=None
        )
        all_results.append(result)
        tested_tasks.append(task_id)
        tested_count += 1

    # 计算平均指标
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
    summary_path = config.rq3_exp_root / mode_version / "rq3_summary.json"
    summary_path.parent.mkdir(parents=True, exist_ok=True)
    summary.save_to_file(summary_path)

    # 保存详细任务分类信息
    task_classification_path = config.rq3_exp_root / mode_version / "task_classification.json"
    task_classification = {
        "total_tasks": len(dsl_files),
        "tested_tasks": {
            "count": len(tested_tasks),
            "tasks": tested_tasks
        },
        "ignored_checker_tasks": {
            "count": len(ignored_checker_tasks),
            "tasks": ignored_checker_tasks,
            "checkers": list(set([task.split('/')[1] for task in ignored_checker_tasks]))
        },
        "no_results_tasks": {
            "count": len(no_results_tasks),
            "tasks": no_results_tasks
        },
        "failed_tasks": {
            "count": len(failed_tasks),
            "tasks": failed_tasks
        }
    }
    with open(task_classification_path, 'w', encoding='utf-8') as f:
        json.dump(task_classification, f, indent=2, ensure_ascii=False)
    _logger.info(f"任务分类详情: {task_classification_path}")

    # 打印统计信息
    _logger.info("\n" + "=" * 80)
    _logger.info(f"RQ3统计完成 ({mode_version})!")
    _logger.info("=" * 80)
    _logger.info(f"总DSL文件数: {len(dsl_files)}")
    _logger.info(f"成功统计: {tested_count}")
    _logger.info(f"跳过: {skipped_count}")
    _logger.info(f"  - 忽略的checker: {len(ignored_checker_tasks)}")
    _logger.info(f"  - 无检测结果: {len(no_results_tasks)}")
    _logger.info(f"失败: {failed_count}")
    _logger.info("")
    _logger.info("平均指标（基于成功统计）:")
    _logger.info(f"  Precision: {avg_precision:.4f}")
    _logger.info(f"  Recall: {avg_recall:.4f}")
    _logger.info(f"  F1: {avg_f1:.4f}")
    _logger.info(f"  平均FP数量: {avg_fp_count:.2f}")
    _logger.info("")
    _logger.info(f"汇总报告: {summary_path}")
    _logger.info("=" * 80)

    # 输出详细的任务列表
    _logger.info("\n" + "=" * 80)
    _logger.info("详细任务分类:")
    _logger.info("=" * 80)

    _logger.info(f"\n成功统计的任务 ({len(tested_tasks)}):")
    for task in tested_tasks:
        _logger.info(f"  ✓ {task}")

    if ignored_checker_tasks:
        _logger.info(f"\n被过滤的任务 (checker在忽略列表) ({len(ignored_checker_tasks)}):")
        for task in ignored_checker_tasks:
            _, checker, _, _ = task.split('/')
            _logger.info(f"  - {task} (checker: {checker})")

    if no_results_tasks:
        _logger.info(f"\n跳过的任务 (无检测结果) ({len(no_results_tasks)}):")
        for task in no_results_tasks:
            _logger.info(f"  × {task}")

    if failed_tasks:
        _logger.info(f"\n失败的任务 ({len(failed_tasks)}):")
        for task in failed_tasks:
            _logger.info(f"  ! {task}")

    _logger.info("=" * 80)

    return summary


# ============================================================================
# 主函数
# ============================================================================

def final_rq3_exp(
    dataset_commit_root: Path,
    final_dsl_root: Path,
    config: ExperimentConfig,
    engine_path: Path,
    case_selections: Dict[str, Dict[str, str]]
) -> RQ3Summary:
    """
    运行final_version DSL的RQ3实验

    Args:
        dataset_commit_root: commit根目录
        final_dsl_root: final DSL根目录
        config: 实验配置
        engine_path: Kirin引擎路径
        case_selections: 预先生成的case选择字典

    Returns:
        RQ3Summary对象
    """
    # 扫描所有DSL文件
    dsl_files = scan_dsl_files(final_dsl_root)

    # 处理每个DSL文件
    all_results = []
    tested_count = 0
    skipped_count = 0
    failed_count = 0

    for i, (task_id, dsl_path) in enumerate(dsl_files, 1):
        _logger.info(f"\n[{i}/{len(dsl_files)}] Processing task: {task_id}")

        # 从文件名提取case1和case2
        dsl_filename = dsl_path.name  # 例如: 1_2.kirin
        if not dsl_filename.endswith('.kirin'):
            _logger.warning(f"Task {task_id}: Invalid DSL filename: {dsl_filename}")
            continue

        # 提取case1和case2
        base_name = dsl_path.stem  # 去掉扩展名
        parts = base_name.split('_')
        if len(parts) != 2:
            _logger.warning(f"Task {task_id}: Invalid DSL filename format: {dsl_filename}")
            continue

        case1, case2 = parts

        # 从预先生成的选择中获取test_case
        if task_id not in case_selections:
            _logger.warning(f"Task {task_id}: No test case selection available, skipping")
            # 创建一个skipped结果
            result = RQ3TaskResult(
                task_id=task_id,
                dsl_file=dsl_filename,
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

        # 运行检测
        result = run_detection_on_test_case(
            dsl_path,
            task_id,
            case1,
            case2,
            test_case,
            config,
            engine_path,
            mode_version="final_version"
        )

        all_results.append(result)

        if result.status == "success":
            tested_count += 1
        elif result.status == "failed":
            failed_count += 1

    # 生成汇总报告
    _logger.info("\n" + "=" * 80)
    _logger.info("Generating summary report...")
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
    summary_path = config.rq3_exp_root / "final_version" / "rq3_summary.json"
    summary_path.parent.mkdir(parents=True, exist_ok=True)
    summary.save_to_file(summary_path)

    # 打印统计信息
    _logger.info("\n" + "=" * 80)
    _logger.info("RQ3实验完成!")
    _logger.info("=" * 80)
    _logger.info(f"总DSL文件数: {len(dsl_files)}")
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


def origin_rq3_exp(
    dataset_commit_root: Path,
    origin_dsl_root: Path,
    config: ExperimentConfig,
    engine_path: Path,
    case_selections: Dict[str, Dict[str, str]]
) -> RQ3Summary:
    """
    运行origin DSL的RQ3实验

    Args:
        dataset_commit_root: commit根目录
        origin_dsl_root: origin DSL根目录
        config: 实验配置
        engine_path: Kirin引擎路径
        case_selections: 预先生成的case选择字典

    Returns:
        RQ3Summary对象
    """
    # 扫描所有DSL文件
    dsl_files = scan_dsl_files(origin_dsl_root)

    # 处理每个DSL文件
    all_results = []
    tested_count = 0
    skipped_count = 0
    failed_count = 0

    for i, (task_id, dsl_path) in enumerate(dsl_files, 1):
        _logger.info(f"\n[{i}/{len(dsl_files)}] Processing task: {task_id}")

        dsl_filename = dsl_path.name
        if not dsl_filename.endswith('.kirin'):
            _logger.warning(f"Task {task_id}: Invalid DSL filename: {dsl_filename}")
            continue

        # Origin格式: DSL文件名是 {case1}.kirin
        # case1和case2应该从case_selections中获取
        if task_id not in case_selections:
            _logger.warning(f"Task {task_id}: No test case selection available, skipping")
            # 无法确定case1和case2，直接跳过
            result = RQ3TaskResult(
                task_id=task_id,
                dsl_file=dsl_filename,
                case1=None,
                case2=None,
                test_case=None,
                detection_metrics=None,
                labeled_result_path=None,
                status="skipped",
                error_message="No test case selection available"
            )
            all_results.append(result)
            skipped_count += 1
            continue

        # 从case_selections中获取case1, case2, test_case
        selection = case_selections[task_id]
        case1 = selection["case1"]
        case2 = selection["case2"]
        test_case = selection["case3"]
        _logger.info(f"Task {task_id}: case1={case1}, case2={case2}, test_case={test_case}")

        # 运行检测
        result = run_detection_on_test_case(
            dsl_path,
            task_id,
            case1,
            case2,
            test_case,
            config,
            engine_path,
            mode_version="origin_version"
        )

        all_results.append(result)

        if result.status == "success":
            tested_count += 1
        elif result.status == "failed":
            failed_count += 1

    # 生成汇总报告
    _logger.info("\n" + "=" * 80)
    _logger.info("Generating summary report...")
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
    summary_path = config.rq3_exp_root / "origin_version" / "rq3_summary.json"
    summary_path.parent.mkdir(parents=True, exist_ok=True)
    summary.save_to_file(summary_path)

    # 打印统计信息
    _logger.info("\n" + "=" * 80)
    _logger.info("RQ3实验完成!")
    _logger.info("=" * 80)
    _logger.info(f"总DSL文件数: {len(dsl_files)}")
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
    # 解析命令行参数
    parser = argparse.ArgumentParser(description="RQ3实验：测试DSL的泛化能力")
    parser.add_argument(
        "--engine-path",
        type=str,
        default="D:/envs/kirin-cli-1.0.8_sp06-jackofext-obfuscate.jar",
        help="Kirin引擎路径"
    )
    parser.add_argument(
        "--mode",
        type=str,
        choices=["final", "origin"],
        default="final",
        help="实验模式: final (最终版本DSL) 或 origin (原始DSL)"
    )
    parser.add_argument(
        "--only-summary",
        action="store_true",
        help="只生成汇总报告(不运行检测),从已有的检测结果统计"
    )

    args = parser.parse_args()
    engine_path = Path(args.engine_path)
    mode = args.mode
    only_summary = args.only_summary

    if not only_summary and not engine_path.exists():
        _logger.error(f"Kirin引擎不存在: {engine_path}")
        return 1

    _logger.info("=" * 80)
    if only_summary:
        _logger.info("RQ3实验：生成汇总报告(仅统计)")
    else:
        _logger.info("RQ3实验：测试DSL的泛化能力")
    _logger.info("=" * 80)

    # 初始化配置
    config = ExperimentConfig()
    dataset_commit_root = config.commit_root
    final_dsl_root = config.final_dsl_root
    origin_dsl_root = config.ori_dsl_root

    _logger.info(f"Dataset commit root: {dataset_commit_root}")
    _logger.info(f"Final DSL root: {final_dsl_root}")
    _logger.info(f"Origin DSL root: {origin_dsl_root}")
    _logger.info(f"Mode: {mode}")
    _logger.info(f"Only summary: {only_summary}")
    _logger.info("")

    # 加载case选择
    _logger.info("Loading test case selections...")
    case_selections = load_case_selections(config)
    if case_selections is None:
        _logger.error("Case selections not found! Please run detection first or generate case selections.")
        return 1
    _logger.info(f"Loaded {len(case_selections)} test case selections")
    _logger.info("")

    # 运行相应的实验
    if only_summary:
        # 只生成汇总报告
        if mode == "final":
            _logger.info("Generating summary for final_version DSLs...")
            generate_summary_from_results(final_dsl_root, config, "final_version", case_selections)
        elif mode == "origin":
            _logger.info("Generating summary for origin DSLs...")
            generate_summary_from_results(origin_dsl_root, config, "origin_version", case_selections)
    else:
        # 运行完整检测+统计
        if mode == "final":
            _logger.info("Running RQ3 on final_version DSLs...")
            final_rq3_exp(dataset_commit_root, final_dsl_root, config, engine_path, case_selections)
        elif mode == "origin":
            _logger.info("Running RQ3 on ori_dsl DSLs...")
            origin_rq3_exp(dataset_commit_root, origin_dsl_root, config, engine_path, case_selections)

    return 0


if __name__ == '__main__':
    exit(main())