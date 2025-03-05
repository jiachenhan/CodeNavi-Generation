from pathlib import Path
from typing import Generator

from interface.java.run_java_api import genpat_detect_all


def get_repo_cases(_path: Path) -> Generator[Path, None, None]:
    for _checker in _path.iterdir():
        if not _checker.is_dir():
            continue
        for group in _checker.iterdir():
            if not group.is_dir():
                continue
            for case in group.iterdir():
                if not case.is_dir():
                    continue
                yield case
                break

def get_pattern_path(_repo_case_path: Path, _data_path: Path) -> Path:
    case_name = _repo_case_path.stem
    group_name = _repo_case_path.parent.stem
    checker_name = _repo_case_path.parent.parent.stem

    data_case = _data_path / checker_name / group_name / case_name
    return data_case


def get_result_path(_repo_case_path: Path, _results_path: Path) -> Path:
    case_name = _repo_case_path.stem
    group_name = _repo_case_path.parent.stem
    checker_name = _repo_case_path.parent.parent.stem

    return _results_path / checker_name / group_name / case_name / "result.txt"


def detect_repo(_dataset_path: Path,
                _repos_path:Path,
                _results_path: Path):
    genpat_cmd = Path("/data/jiangjiajun/CodeNavi-DSL/GenPat")
    genpat_jar = genpat_cmd / "GenPat-1.0-SNAPSHOT-runnable.jar"

    cases = get_repo_cases(_repos_path)
    for repo_case in cases:
        print(f"repo_case: {repo_case}")
        data_path = get_pattern_path(repo_case, dataset_path)
        result_path = get_result_path(repo_case, _results_path)

        if not data_path.exists():
            print(f"Invalid data path: {data_path}")
            continue

        _pattern_buggy_path = data_path / "buggy.java"
        _pattern_fixed_path = data_path / "fixed.java"

        repo_path = next(repos_path.iterdir(), None)
        if repo_path is None:
            print("No repo found")
            continue

        genpat_detect_all(30 * 60, _pattern_buggy_path, _pattern_fixed_path, repo_path, result_path, genpat_jar)

def calculate_det_num(_results_path: Path):
    det_num_list = []
    for checker in _results_path.iterdir():
        for group in checker.iterdir():
            for case in group.iterdir():
                result_path = case / "result.txt"
                if not result_path.exists():
                    print(f"Invalid result path: {result_path}")
                    continue

                with open(result_path, "r", encoding="utf-8") as file:
                    lines = file.readlines()
                    det_num = len(lines)
                    det_num_list.append((result_path, det_num))

    total_det_nums = sum([det_num for _, det_num in det_num_list])
    print(f"Total det num: {total_det_nums}")
    print(f"Average det num: {total_det_nums / len(det_num_list)}")
    print("det nums:")
    for _ in det_num_list:
        print(f"{_[0]}: {_[1]}")

if __name__ == '__main__':
    dataset_name = "codeql_sampled_v1"
    dataset_path = Path("/data/jiangjiajun/CodeNavi-DSL/data") / dataset_name
    repos_path = Path("/data/jiangjiajun/CodeNavi-DSL/data") / f"{dataset_name}_repos"

    results_path = Path(f"/data/jiangjiajun/CodeNavi-DSL/GenPat/repo_{dataset_name}")

    # detect_repo(dataset_path, repos_path, results_path)
    calculate_det_num(results_path)
