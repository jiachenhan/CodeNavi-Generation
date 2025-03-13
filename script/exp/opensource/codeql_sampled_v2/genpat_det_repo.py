import json
import random
from pathlib import Path
from typing import Generator, Tuple, Optional

from exp.opensource.statistic_analysis import genpat_repo_statistic
from interface.java.run_java_api import genpat_detect_all
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def get_random_repo_path(_dataset_case_path: Path, _repos_base_path: Path) -> Optional[Path]:
    _case_name = _dataset_case_path.stem
    _group_name = _dataset_case_path.parent.stem
    _checker_name = _dataset_case_path.parent.parent.stem
    _group_repo_path = _repos_base_path / _checker_name / _group_name

    _other_cases = [_case for _case in _group_repo_path.iterdir() if _case.is_dir() and _case_name != _case.stem]
    if not _other_cases:
        _logger.error(f"No other cases in {_group_repo_path}")
        return None
    _random_case_path = random.choice(_other_cases)
    return next(filter(lambda file : file.is_dir(), _random_case_path.iterdir()), None)


def get_code_pair_path(_dataset_case_path: Path) -> Tuple[Path, Path]:
    return _dataset_case_path / "buggy.java", _dataset_case_path / "fixed.java"


def get_dataset_case(_dataset_path: Path) -> Generator[Path, None, None]:
    for checker in _dataset_path.iterdir():
        if not checker.is_dir():
            continue
        for group in checker.iterdir():
            if not group.is_dir():
                continue
            yield random.choice([_case for _case in group.iterdir() if _case.is_dir()])


def get_result_path(_dataset_case_path: Path, _results_path: Path, _scanned_case_num: str) -> Path:
    case_name = _dataset_case_path.stem
    group_name = _dataset_case_path.parent.stem
    checker_name = _dataset_case_path.parent.parent.stem

    return _results_path / checker_name / group_name / f"{case_name}-{_scanned_case_num}" / "result.txt"


def detect_repo(_dataset_path: Path,
                _repos_path: Path,
                _results_path: Path):
    genpat_cmd = Path("/data/jiangjiajun/CodeNavi-DSL/GenPat")
    genpat_jar = genpat_cmd / "GenPat-1.0-SNAPSHOT-runnable.jar"

    for _dataset_case in get_dataset_case(_dataset_path):
        _scanned_repo_path = get_random_repo_path(_dataset_case, _repos_path)
        _logger.info(f"Scanning {_dataset_case} in {_scanned_repo_path}")

        if _scanned_repo_path is None:
            _logger.error(f"No repo found: {_dataset_case}")
            continue

        _buggy_path, _fixed_path = get_code_pair_path(_dataset_case)
        _result_path = get_result_path(_dataset_case, _results_path, _scanned_repo_path.parent.stem)

        if _result_path.parent.parent.exists():
            continue
        genpat_detect_all(30 * 60, _buggy_path, _fixed_path, _scanned_repo_path, _result_path, genpat_jar)



if __name__ == '__main__':
    dataset_name = "codeql_sampled_v2"

    dataset_path = Path("/data/jiangjiajun/CodeNavi-DSL/data") / dataset_name
    repos_path = Path("/data/jiangjiajun/DSL-AutoDebug/data") / f"{dataset_name}_all_repos_test"

    results_path = Path(f"/data/jiangjiajun/CodeNavi-DSL/GenPat/result_trans_repo_{dataset_name}")

    # reports_path = Path(f"/data/jiangjiajun/DSL-AutoDebug/data/{dataset_name}_reports")

    result_store_path = results_path / "genpat_result_store.json"

    # detect_repo(dataset_path, repos_path, results_path)
    results = genpat_repo_statistic(dataset_path, results_path)

    with open(result_store_path, "w", encoding="utf-8") as file:
        json.dump(results, file, indent=4)
