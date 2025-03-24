import json
import platform
import random
import stat
from pathlib import Path
from typing import Generator

from exp.opensource.statistic_analysis import navi_repo_statistic
from interface.java.run_java_api import kirin_engine
from utils.config import LoggerConfig

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
    engine_path = Path("D:/envs/kirin-cli-1.0.8_sp06-jackofext-obfuscate.jar")

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
            timeout = kirin_engine(300, engine_path, _dsl_path, pkg, index_result_path)
            if timeout:
                print(f"Timeout in : {_dsl_path}")
                break


if __name__ == '__main__':
    dataset_name = "codeql_sampled_v1"
    dataset_path = Path(f"E:/dataset/Navi/3-23-sampled-datasets/{dataset_name}")

    query_base_path = Path(f"D:/workspace/CodeNavi-Generation/07dsl/3-21-all-sampled/{dataset_name}")
    repos_path = Path(f"E:/dataset/Navi/{dataset_name}_repos")
    results_path = Path(f"E:/dataset/Navi/result_trans_repo_{dataset_name}")

    detect_repo(query_base_path, repos_path, results_path)

    # sat_reports_path = Path("D:/datas/pmd_sampled_v1_reports")
    result_store_path = results_path / "navi_result_store.json"

    # results = navi_repo_statistic(dataset_path, results_path)

    # with open(result_store_path, "w", encoding="utf-8") as file:
    #     json.dump(results, file, indent=4)
