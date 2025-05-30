import json
import re
from pathlib import Path
from typing import Callable

from exp.opensource.statistic_analysis import collect_navi_result
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def collect_genpat_warnings(genpat_results_path: Path) -> list:
    result_file = genpat_results_path / "result.txt"
    lines = open(result_file, 'r', encoding="ISO-8859-1").readlines()
    ret_warnings = []
    for line in lines:
        line = line.strip()
        line = re.sub(r"\t+", " ", line)
        if len(line) == 0 or line.startswith("Empty Pattern"):
            continue
        info = line.split("det ")[1].strip()
        file_path = info.split(" ")[0].replace("\\", "/")
        sig_name = " ".join(info.split(" ")[1:]).split(" ")[1].split("[")[0]
        ret_type = info.split(" ")[1].strip()
        params = " ".join(info.split(" ")[1:]).split("[")[1].split("]")[0].strip()
        ret_warnings.append((file_path, sig_name, ret_type, params))
    return ret_warnings


def get_baseline_warnings(baseline_case_warning_path: Path) -> list:
    lines = open(baseline_case_warning_path, 'r', encoding="ISO-8859-1").readlines()
    ret_warnings = []
    for line in lines:
        if not line.startswith("hash#"):
            continue
        _, file_path, begin_line, end_line, sig = line.strip().split("#")
        params = sig.split("(")[1].split(")")[0].strip()
        sig_name = " ".join(sig.split(" ")[1:]).split("(")[0].strip()
        ret_warnings.append((file_path, sig_name, params, begin_line, end_line))
    return ret_warnings


def calculate_metrics(_baseline_warnings: list,
                      _genpat_warnings: list,
                      match_func: Callable[[tuple, tuple], bool]) -> tuple:
    matched = [False] * len(_baseline_warnings)
    _tp = 0
    _fp = 0

    for _genpat_warning in _genpat_warnings:
        match_idx = next((i for i, gt in enumerate(_baseline_warnings) if not matched[i]
                          and match_func(gt, _genpat_warning)), None)
        if match_idx is not None:
            matched[match_idx] = True
            _tp += 1
        else:
            _fp += 1
    _fn = len(_baseline_warnings) - sum(matched)

    _precision = _tp / (_tp + _fp) if (_tp + _fp) > 0 else 0
    _recall = _tp / (_tp + _fn) if (_tp + _fn) > 0 else 0
    _f1_score = (2 * _precision * _recall / (_precision + _recall)) if (_precision + _recall) > 0 else 0
    return _tp, _fp, _fn, _precision, _recall, _f1_score


def statistic_navi(_dataset_name, _navi_scan_warnings_path: Path, _baseline_warnings_path: Path) -> None:
    result_path = Path(f"E:/dataset/Navi/rq2_navi_{_dataset_name}.json")

    _result_dict = {}
    for _checker_path in _navi_scan_warnings_path.iterdir():
        if not _checker_path.is_dir():
            continue
        _result_dict[_checker_path.stem] = []
        for _group_path in _checker_path.iterdir():
            _logger.info(f"In navi statistics: {_group_path}")
            _case_result_dict = {}
            _navi_case_path = next(_group_path.iterdir(), None)
            if not _navi_case_path:
                _logger.error(f"No case in {_group_path}")
                continue
            _case_result_dict["group"] = str(_group_path.stem)
            _case_result_dict["case_pair"] = str(_navi_case_path.stem)
            scanned_case = _navi_case_path.stem.split("-")[-1]
            _baseline_case_path = next((_baseline_warnings_path / _checker_path.stem /
                                        _group_path.stem / scanned_case).glob("*.txt"), None)
            if not _baseline_case_path:
                _logger.error(f"No baseline warning in {_baseline_case_path}")
                continue

            # statistic warnings
            # (filename(relative), func name, params, begin line, end line)
            _baseline_warnings = get_baseline_warnings(_baseline_case_path)
            # (func name, filename(abs), line number)
            _navi_warnings = collect_navi_result(_navi_case_path)

            def matched_warnings(_baseline_warning: tuple, _navi_warning: tuple) -> bool:
                file_name, func_name, params, begin_line, end_line = _baseline_warning
                navi_func_name, navi_file_name, navi_line_num = _navi_warning
                renamed_file_name = navi_file_name.replace("src/untest", "src/test")
                return ((file_name in navi_file_name or file_name in renamed_file_name) and func_name == navi_func_name
                        and begin_line <= navi_line_num <= end_line)

            tp, fp, fn, precision, recall, f1_score = calculate_metrics(_baseline_warnings, _navi_warnings,
                                                                        matched_warnings)
            _case_result_dict["tp"] = tp
            _case_result_dict["fp"] = fp
            _case_result_dict["fn"] = fn
            _case_result_dict["precision"] = round(precision, 4)
            _case_result_dict["recall"] = round(recall, 4)
            _case_result_dict["f1_score"] = round(f1_score, 4)

            _result_dict[_checker_path.stem].append(_case_result_dict)

    with open(result_path, 'w', encoding="utf-8") as f:
        json.dump(_result_dict, f, indent=4, ensure_ascii=False)
    pass


def statistic_genpat(_dataset_name, _genpat_scan_warnings_path: Path, _baseline_warnings_path: Path) -> None:
    result_path = Path(f"E:/dataset/Navi/rq2_genpat_{_dataset_name}.json")

    _result_dict = {}
    for _checker_path in _genpat_scan_warnings_path.iterdir():
        if not _checker_path.is_dir():
            continue
        _result_dict[_checker_path.stem] = []
        for _group_path in _checker_path.iterdir():
            _logger.info(f"In genpat statistics: {_group_path}")
            _case_result_dict = {}
            _genpat_case_path = next(_group_path.iterdir(), None)
            if not _genpat_case_path:
                _logger.error(f"No case in {_group_path}")
                continue
            _case_result_dict["group"] = str(_group_path.stem)
            _case_result_dict["case_pair"] = str(_genpat_case_path.stem)
            scanned_case = _genpat_case_path.stem.split("-")[-1]
            _baseline_case_path = next((_baseline_warnings_path / _checker_path.stem /
                                        _group_path.stem / scanned_case).glob("*.txt"), None)
            if not _baseline_case_path:
                _logger.error(f"No baseline warning in {_baseline_case_path}")
                continue

            # statistic warnings
            # (filename(relative), func name, params, begin line, end line)
            _baseline_warnings = get_baseline_warnings(_baseline_case_path)
            # (filename(abs), func name, return type, param types)
            _genpat_warnings = collect_genpat_warnings(_genpat_case_path)

            def matched_warnings(_baseline_warning: tuple, _genpat_warning: tuple) -> bool:
                file_name, func_name, params, begin_line, end_line = _baseline_warning
                genpat_file_name, genpat_func_name, return_type, param_types = _genpat_warning
                return (file_name in genpat_file_name and func_name == genpat_func_name
                        and params == param_types)


            tp, fp, fn, precision, recall, f1_score = calculate_metrics(_baseline_warnings, _genpat_warnings,
                                                                        matched_warnings)
            _case_result_dict["tp"] = tp
            _case_result_dict["fp"] = fp
            _case_result_dict["fn"] = fn
            _case_result_dict["precision"] = round(precision, 4)
            _case_result_dict["recall"] = round(recall, 4)
            _case_result_dict["f1_score"] = round(f1_score, 4)

            _result_dict[_checker_path.stem].append(_case_result_dict)

    with open(result_path, 'w', encoding="utf-8") as f:
        json.dump(_result_dict, f, indent=4, ensure_ascii=False)
    pass


if __name__ == "__main__":
    dataset_name = "codeql_sampled_v1"
    navi_scan_warnings_path = Path(f"E:/dataset/Navi/4_23_result_trans_repo_{dataset_name}")
    genpat_scan_warnings_path = Path(f"E:/dataset/Navi/4_23_consistent_genpat_result_trans_repo_{dataset_name}")
    baseline_warnings_path = Path(f"E:/dataset/Navi/{dataset_name}_repos111")

    statistic_navi(dataset_name, navi_scan_warnings_path, baseline_warnings_path)
    # statistic_genpat(dataset_name, genpat_scan_warnings_path, baseline_warnings_path)
