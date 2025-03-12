import json
import re
from collections.abc import Callable
from pathlib import Path
from typing import Generator

from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def genpat_static_fps_recall(result_path: Path, _data_path: Path) -> dict:
    result = {"all_scanned": 0, "no_pattern": False,
              "fp": 0, "recall": False}
    if not result_path.exists():
        _logger.error(f"No result found: {result_path}")
        return result
    with open(result_path, "r", encoding="utf-8") as file:
        for index, line in enumerate(file):
            if index == 0 and "Empty Pattern" in line:
                result["no_pattern"] = True
                return result
            result["all_scanned"] += 1
            detect_path = line.split("\t")[1]
            method_sig = line.split("\t")[2]

            _fixed_code, tp_path = extract_tp_code_path(_data_path)
            if method_sig.split("[")[0] in _fixed_code and tp_path in detect_path:
                result["recall"] = True
            else:
                result["fp"] += 1
    return result


def extract_tp_code_path(_data_path):
    _fixed_path = _data_path / "fixed.java"
    _info_path = _data_path / "info.json"
    _pattern_source = json.loads(open(_info_path, "r", encoding="utf-8").read()).get("pattern_source")
    tp_path = re.search(r'#([^#]*\.java)', _pattern_source).group(1)
    _fixed_code = open(_fixed_path, "r", encoding="utf-8").read()
    return _fixed_code, tp_path


def genpat_repo_statistic(_dataset_path: Path, _results_path: Path):
    _results = []
    for result_path in _results_path.rglob("result.txt"):
        _logger.info(f"In statistics: {result_path}")
        scanned_case_num = result_path.parent.stem.split("-")[-1]
        group_name = result_path.parents[1].stem
        checker_name = result_path.parents[2].stem

        _data_path = _dataset_path / checker_name / group_name / scanned_case_num

        result = genpat_static_fps_recall(result_path, _data_path)
        _results.append({"result_path": str(result_path), "result": result})
    return _results

###################################################################################

def collect_result(reports_path: Path) -> list:
    result = []
    for report_path in reports_path.rglob("*.xml"):
        slice_result = xml_collect_errors(report_path)
        result.extend(slice_result)

    return result


def navi_static_fps_recall(scanned_result: list, _data_path: Path) -> dict:
    result = {"all_scanned": len(scanned_result), "fp": 0, "recall": False}

    _fixed_code, tp_path = extract_tp_code_path(_data_path)

    for method_sig, detect_path in scanned_result:
        if method_sig.split("[")[0] in _fixed_code and tp_path in detect_path:
            result["recall"] = True
        else:
            result["fp"] += 1
    return result


def collect_scanned_cases(result_path: Path) -> Generator[Path, None, None]:
    for checker in result_path.iterdir():
        if not checker.is_dir():
            continue
        for group in checker.iterdir():
            if not group.is_dir():
                continue
            for case_scanned in group.iterdir():
                if not case_scanned.is_dir():
                    continue
                yield case_scanned

def navi_repo_statistic(_dataset_path: Path, _results_path: Path):
    _results = []
    for result_path in collect_scanned_cases(_results_path):
        _logger.info(f"In statistics: {result_path}")
        scanned_case_num = result_path.stem.split("-")[-1]
        group_name = result_path.parent.stem
        checker_name = result_path.parents[1].stem

        scanned_result = collect_result(result_path)
        _data_path = _dataset_path / checker_name / group_name / scanned_case_num


        result = navi_static_fps_recall(scanned_result, _data_path)
        _results.append({"result_path": str(result_path), "result": result})
    return _results

def xml_collect_errors(_output_path: Path) -> list:
    _results = []
    if not _output_path.exists():
        return []
    import xml.etree.ElementTree as ET
    xml_root = ET.parse(_output_path)
    all_error = xml_root.find("errors").findall("error")
    for error in all_error:
        defect_info = error.find("defectInfo")
        func_name = defect_info.find("function").text
        file_name = defect_info.find("fileName").text.replace("\\", "/")
        _results.append((func_name, file_name))
    return _results


def xml_count_errors(_output_path: Path) -> int:
    if not _output_path.exists():
        return 0

    import xml.etree.ElementTree as ET
    xml_root = ET.parse(_output_path)
    all_error = xml_root.find("errors").findall("error")
    return len(all_error)
