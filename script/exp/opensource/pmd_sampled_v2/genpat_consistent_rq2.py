import json
from pathlib import Path
from typing import Tuple

from exp.opensource.statistic_analysis import genpat_repo_statistic
from interface.java.run_java_api import genpat_detect_all
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def get_det_pair(_navi_result_path: Path):
    for _checker in _navi_result_path.iterdir():
        if not _checker.is_dir():
            continue
        for group in _checker.iterdir():
            if not group.is_dir():
                continue
            _case_scanned = next(group.iterdir(), None)
            if _case_scanned is None:
                _logger.error(f"No case in {group}")

            _case_name, _scanned_name = _case_scanned.stem.split("-")
            yield _checker.stem, group.stem, _case_name, _scanned_name


def get_code_pair_path(_dataset_case_path: Path) -> Tuple[Path, Path]:
    return _dataset_case_path / "buggy.java", _dataset_case_path / "fixed.java"

def get_result_path(_dataset_case_path: Path, _results_path: Path, _scanned_case_num: str) -> Path:
    case_name = _dataset_case_path.stem
    group_name = _dataset_case_path.parent.stem
    checker_name = _dataset_case_path.parent.parent.stem

    return _results_path / checker_name / group_name / f"{case_name}-{_scanned_case_num}" / "result.txt"


def detect_repo(_dataset_path: Path, _repos_base_path: Path, _navi_result_path: Path, _results_path: Path):
    genpat_cmd = Path("D:/envs/GenPat")
    genpat_jar = genpat_cmd / "GenPat-1.0-SNAPSHOT-runnable.jar"

    for _checker_name, _group_name, _case_name, _scanned_name in get_det_pair(_navi_result_path):
        _dataset_case_path = _dataset_path / _checker_name / _group_name / _case_name
        _scanned_case_path = _repos_base_path / _checker_name / _group_name / _scanned_name
        _scanned_repo_path = next(_scanned_case_path.iterdir(), None)
        if not _dataset_case_path.exists():
            _logger.error(f"{_dataset_case_path} does not exist")
            continue

        if not _scanned_repo_path:
            _logger.error(f"{_scanned_repo_path} does not exist")
            continue

        _logger.info(f"Scanning {_dataset_case_path} in {_scanned_repo_path}")

        _buggy_path, _fixed_path = get_code_pair_path(_dataset_case_path)
        _result_path = get_result_path(_dataset_case_path, _results_path, _scanned_case_path.stem)
        genpat_detect_all(30 * 60, _buggy_path, _fixed_path, _scanned_repo_path, _result_path, genpat_jar)


if __name__ == '__main__':
    dataset_name = "pmd_sampled_v2"

    dataset_path = Path(f"E:/dataset/Navi/3-23-sampled-datasets/{dataset_name}")
    repos_path = Path(f"E:/dataset/Navi/{dataset_name}_repos")

    navi_path = Path(f"E:/dataset/Navi/4_23_result_trans_repo_{dataset_name}")
    results_path = Path(f"E:/dataset/Navi/4_23_consistent_genpat_result_trans_repo_{dataset_name}")

    result_store_path = results_path / "genpat_result_store.json"

    detect_repo(dataset_path, repos_path, navi_path, results_path)
    results = genpat_repo_statistic(dataset_path, results_path)

    with open(result_store_path, "w", encoding="utf-8") as file:
        json.dump(results, file, indent=4)
