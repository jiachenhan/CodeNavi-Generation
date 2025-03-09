import json
import platform
import random
import stat
from pathlib import Path
from typing import Generator

from exp.opensource.codeql_sampled_v1.genpat_det_repo import collect_report
from interface.java.run_java_api import kirin_engine
from utils.config import get_dsl_base_path, LoggerConfig

_logger = LoggerConfig.get_logger(__name__)

def get_random_repo_path(_dsl_path: Path, _repos_base_path: Path) -> Path:
    _case_name = _dsl_path.stem
    _group_name = _dsl_path.parent.stem
    _checker_name = _dsl_path.parent.parent.stem
    _group_repo_path = _repos_base_path / _checker_name / _group_name

    _random_case_path = random.choice([_case for _case in _group_repo_path.iterdir() if _case.is_dir() and _case_name != _case.stem])
    return next(_random_case_path.iterdir(), None)


def get_dsl_paths(_query_path: Path) -> Generator[Path, None, None]:
    for checker in _query_path.iterdir():
        if not checker.is_dir():
            continue
        for group in checker.iterdir():
            if not group.is_dir():
                continue
            dsl_path = next(group.glob("*.kirin"), None)
            if dsl_path is not None:
                yield dsl_path


def get_result_path(_dsl_path: Path, _results_path: Path, _scanned_case_num: str) -> Path:
    case_name = _dsl_path.stem
    group_name = _dsl_path.parent.stem
    checker_name = _dsl_path.parent.parent.stem

    return _results_path / checker_name / group_name / f"{case_name}-{_scanned_case_num}"


def split_java_package(dir_path: Path) -> Generator[Path, None, None]:
    if platform.system() == "Windows":
        try:
            attrs = dir_path.stat().st_file_attributes
            if (attrs & (stat.FILE_ATTRIBUTE_HIDDEN | stat.FILE_ATTRIBUTE_SYSTEM)) != 0:
                return
        except AttributeError:
            return

    if dir_path.name.startswith("."):
        return

    if dir_path.is_file():
        if dir_path.name.endswith(".java"):
            yield dir_path
        return

    all_java = all(child.is_file() and child.name.endswith(".java") for child in dir_path.iterdir())
    if all_java:
        yield dir_path
    else:
        for child in dir_path.iterdir():
            yield from split_java_package(child)


def detect_repo(_query_base_path: Path,
                _repos_path: Path,
                _results_path: Path):
    engine_path = Path("D:/env/kirin-cli-1.0.8_sp06-jackofall.jar")

    for _dsl_path in get_dsl_paths(_query_base_path):
        _scanned_repo_path = get_random_repo_path(_dsl_path, _repos_path)
        if _scanned_repo_path is None:
            print("No repo found")
            continue
        _result_path = get_result_path(_dsl_path, _results_path, _scanned_repo_path.parent.stem)

        # if _result_path.exists():
        #     continue

        for index, pkg in enumerate(split_java_package(_scanned_repo_path)):
            index_result_path = _result_path / f"{index}_error_kirin"
            timeout = kirin_engine(60, engine_path, _dsl_path, pkg, index_result_path)
            if timeout:
                print(f"Timeout in : {_dsl_path}")
                break


def collect_result(reports_path: Path) -> list:
    result = []
    for report_path in reports_path.rglob("*.xml"):
        slice_result = xml_collect_errors(report_path)
        result.extend(slice_result)

    return result


def static_pre_recall(scanned_result: list, report_result: list) -> dict:
    result = {"all_scanned": 0, "report_all": len(report_result),
              "tp": 0, "fp": 0, "fn": 0,
              "pre": 0.0, "recall": 0.0}
    if not scanned_result:
        result["fn"] = len(report_result)
        return result

    result["all_scanned"] = len(scanned_result)
    for method_sig, detect_path in scanned_result:
        if any([report.get("path") in detect_path
                and method_sig in report.get("signature")
                for report in report_result]):
            result["tp"] += 1
        else:
            result["fp"] += 1

    result["fn"] = result["report_all"] - result["tp"]
    if result["all_scanned"] != 0:
        result["pre"] = result["tp"] / (result["tp"] + result["fp"])
        result["recall"] = result["tp"] / (result["tp"] + result["fn"])
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

def statistic(_repos_path: Path, _results_path: Path, _reports_name: str):
    _results = []
    for result_path in collect_scanned_cases(_results_path):
        _logger.info(f"In statistics: {result_path}")
        scanned_case_num = result_path.stem.split("-")[-1]
        group_name = result_path.parent.stem
        checker_name = result_path.parents[1].stem

        report_path = _repos_path / checker_name / group_name / scanned_case_num / _reports_name

        scanned_result = collect_result(result_path)
        report_result = collect_report(report_path)

        result = static_pre_recall(scanned_result, report_result)
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


if __name__ == '__main__':
    dataset_name = "pmd_sampled_v1"
    query_base_path = Path("D:/workspace/CodeNavi-Generation/07dsl/") / dataset_name

    repos_path = Path("/data/jiangjiajun/CodeNavi-DSL/data") / f"{dataset_name}_repos"

    results_path = Path(f"/data/jiangjiajun/CodeNavi-DSL/GenPat/repo_{dataset_name}")
    sat_reports_path = Path("D:/datas/pmd_sampled_v1_reports")
    result_store_path = results_path / "result_store.json"


    # detect_repo(query_base_path, repos_path, results_path)

    results = statistic(sat_reports_path, results_path, "pmd_warnings.txt")

    print(results)

    # with open(result_store_path, "w", encoding="utf-8") as file:
    #     json.dump(results, file, indent=4)