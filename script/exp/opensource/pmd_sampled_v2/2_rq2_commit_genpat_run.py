import json
import re
from pathlib import Path
from typing import Optional, Tuple, Callable

from interface.java.run_java_api import genpat_detect_all
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def get_pattern_case(_base_path: Path, _checker_name: str, _group_name: str) -> Optional[str]:
    kirin_group_path = _base_path / _checker_name / _group_name
    kirin_path = next(kirin_group_path.glob("*.kirin"), None)
    if kirin_path is not None:
        return kirin_path.stem
    return None


def get_code_pair_path(_dataset_pair_path: Path,
                       _checker_name: str,
                       _group_name: str,
                       _case_name: str) -> Tuple[Path, Path]:
    return (_dataset_pair_path / _checker_name / _group_name / _case_name / "buggy.java",
            _dataset_pair_path / _checker_name / _group_name / _case_name / "fixed.java")


def run_genpat(_sat_name: str,
               _dataset_pair_path: Path,
               _pattern_base_path: Path,
               _scanned_base_path: Path,
               _result_base_path: Path,
               _genpat_jar: Path):
    for _checker in _scanned_base_path.iterdir():
        for _group in _checker.iterdir():
            for _scanned_commit_case in _group.iterdir():
                if not (_scanned_commit_case / f"{_sat_name}_final.xml").exists():
                    # 选择特定的case
                    continue

                _checker_name = _checker.stem
                _group_name = _group.stem
                _scanned_case_name = _scanned_commit_case.stem
                _pattern_case_name = get_pattern_case(_pattern_base_path, _checker_name, _group_name)

                _logger.info(f"c:{_checker_name}, g:{_group_name}")
                _logger.info(f"Pattern case: {_pattern_case_name}, Scanned case: {_scanned_case_name}")

                _pattern_before_path, _pattern_after_path = get_code_pair_path(_dataset_pair_path,
                                                                               _checker_name,
                                                                               _group_name,
                                                                               _pattern_case_name)

                _result_path = (_result_base_path / _checker_name / _group_name /
                                f"{_pattern_case_name}-{_scanned_case_name}" / "result.txt")

                _scanned_path = _scanned_commit_case / "after"

                genpat_detect_all(30 * 60, _pattern_before_path, _pattern_after_path, _scanned_path,
                                  _result_path, genpat_jar)


def get_ground_truth(_ground_truth_path: Path) -> list:
    with open(_ground_truth_path, 'r', encoding='utf-8') as f:
        ground_truth = json.load(f)
        return ground_truth


def collect_genpat_warnings(_genpat_results_group_path: Path) -> list:
    if not _genpat_results_group_path.exists():
        _logger.error(f"GenPat result file not found: {_genpat_results_group_path}")
        return []
    _result_case_path = next(_genpat_results_group_path.iterdir(), None)
    if not _result_case_path:
        _logger.error(f"GenPat result file not found: {_genpat_results_group_path}")
        return []
    result_file = _result_case_path / "result.txt"
    if not result_file.exists():
        _logger.error(f"GenPat result file not found: {result_file}")
        return []

    lines = open(result_file, 'r', encoding="ISO-8859-1").readlines()
    ret_warnings = []
    for line in lines:
        line = line.strip()
        if len(line) == 0 or line.startswith("Empty Pattern"):
            continue
        _, file_info, sig_info, line_info = line.split("\t")

        file_path = file_info.replace("\\", "/").split("/")[-1]

        sig_name = sig_info.split(" ")[1].split("[")[0].strip()
        ret_type = sig_info.split(" ")[0].strip()
        params = sig_info.split("[")[1].split("]")[0].strip()
        begin_line = line_info.split("#")[0].strip()
        end_line = line_info.split("#")[1].strip()
        ret_warnings.append((file_path, sig_name, ret_type, params, begin_line, end_line))
    return ret_warnings


def calculate_metrics(_ground_truth_warnings: list,
                      _genpat_warnings: list,
                      match_func: Callable[[dict, tuple], bool]) -> tuple:
    matched = [False] * len(_ground_truth_warnings)
    _tp = 0
    _fp = 0

    for _genpat_warning in _genpat_warnings:
        match_idx = next((i for i, gt in enumerate(_ground_truth_warnings) if not matched[i]
                          and match_func(gt, _genpat_warning)), None)
        if match_idx is not None:
            matched[match_idx] = True
            _tp += 1
        else:
            _fp += 1
    _fn = len(_ground_truth_warnings) - sum(matched)

    _precision = _tp / (_tp + _fp) if (_tp + _fp) > 0 else 0
    _recall = _tp / (_tp + _fn) if (_tp + _fn) > 0 else 0
    _f1_score = (2 * _precision * _recall / (_precision + _recall)) if (_precision + _recall) > 0 else 0
    return _tp, _fp, _fn, _precision, _recall, _f1_score


def statistic_genpat(_sat_name: str, _result_base_path: Path, _scanned_base_path: Path):
    _result_dict = {}
    _result_dict["all_tp"] = 0
    _result_dict["all_fp"] = 0
    _result_dict["all_fn"] = 0

    for _checker in _scanned_base_path.iterdir():
        _result_dict[_checker.stem] = []
        for _group in _checker.iterdir():
            for _scanned_commit_case in _group.iterdir():
                if not (_scanned_commit_case / f"{_sat_name}_final.xml").exists():
                    # 选择特定的case
                    continue

                _ground_truth_path = _scanned_commit_case / "sat_warnings.json"

                _checker_name = _checker.stem
                _group_name = _group.stem
                _result_group_path = _result_base_path / _checker_name / _group_name

                _ground_truth_warnings = get_ground_truth(_ground_truth_path)
                _genpat_warnings = collect_genpat_warnings(_result_group_path)

                def matched_warnings(_ground_truth_warning: dict, _genpat_warning: tuple) -> bool:
                    truth_path = _ground_truth_warning["file"]
                    truth_begin_line = str(_ground_truth_warning["begin_line"])
                    truth_end_line = str(_ground_truth_warning["end_line"])
                    file_path, genpat_func_name, return_type, param_types, begin_line, end_line = _genpat_warning
                    return (truth_path == file_path and truth_begin_line == begin_line
                            and truth_end_line == end_line)

                tp, fp, fn, precision, recall, f1_score = calculate_metrics(_ground_truth_warnings, _genpat_warnings,
                                                                            matched_warnings)

                _case_result_dict = {}
                _case_result_dict["group"] = _group_name
                _case_result_dict["case"] = str(_scanned_commit_case.stem)
                _case_result_dict["tp"] = tp
                _case_result_dict["fp"] = fp
                _case_result_dict["fn"] = fn
                _case_result_dict["precision"] = round(precision, 4)
                _case_result_dict["recall"] = round(recall, 4)
                _case_result_dict["f1_score"] = round(f1_score, 4)

                _result_dict[_checker.stem].append(_case_result_dict)
                _result_dict["all_tp"] += tp
                _result_dict["all_fp"] += fp
                _result_dict["all_fn"] += fn

    _result_dict["all_precision"] = round(_result_dict["all_tp"] / (_result_dict["all_tp"] + _result_dict["all_fp"]), 4)
    _result_dict["all_recall"] = round(_result_dict["all_tp"] / (_result_dict["all_tp"] + _result_dict["all_fn"]), 4)
    _result_dict["all_f1_score"] = round((2 * _result_dict["all_precision"] * _result_dict["all_recall"] /
                                          (_result_dict["all_precision"] + _result_dict["all_recall"])), 4)

    _result_path = _result_base_path / "result.json"
    with open(_result_path, 'w', encoding="utf-8") as f:
        json.dump(_result_dict, f, indent=4, ensure_ascii=False)


if __name__ == "__main__":
    genpat_jar = Path(f"/data/jiangjiajun/CodeNavi-DSL/GenPat/GenPat-1.0-SNAPSHOT-runnable.jar")
    sat_name = "pmd"
    version = "v2"
    dataset_pair_name = f"{sat_name}_sampled_{version}"
    dataset_commits_name = f"{sat_name}_{version}_commits"

    dataset_pair_path = Path(f"/data/jiangjiajun/CodeNavi-DSL/data/{dataset_pair_name}")
    dataset_commits_path = Path(f"/data/jiangjiajun/DSL-AutoDebug/{dataset_commits_name}")
    kirin_dsl_path = Path(f"/data/jiangjiajun/CodeNavi-DSL/CodeNavi-Generation/07dsl/{dataset_pair_name}")

    result_base_path = Path(f"/data/jiangjiajun/CodeNavi-DSL/GenPat/rq2_commit_result/{dataset_commits_name}")

    run_genpat(sat_name, dataset_pair_path, kirin_dsl_path, dataset_commits_path, result_base_path, genpat_jar)

    statistic_genpat(sat_name, result_base_path, dataset_commits_path)
