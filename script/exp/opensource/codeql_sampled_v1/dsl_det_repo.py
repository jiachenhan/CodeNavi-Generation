from pathlib import Path
from typing import Generator

from interface.java.run_java_api import kirin_engine


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

def get_pattern_path(_repo_case_path: Path, _query_path: Path) -> Path:
    case_name = _repo_case_path.stem
    group_name = _repo_case_path.parent.stem
    checker_name = _repo_case_path.parent.parent.stem

    query = _query_path / checker_name / group_name / f"{case_name}.kirin"
    return query


def get_result_path(_repo_case_path: Path, _results_path: Path) -> Path:
    case_name = _repo_case_path.stem
    group_name = _repo_case_path.parent.stem
    checker_name = _repo_case_path.parent.parent.stem

    return _results_path / checker_name / group_name / case_name


def detect_repo(_query_base_path: Path,
                _repos_path: Path,
                _results_path: Path):
    engine_path = Path("D:/env/kirin-cli-1.0.8_sp06-jackofall.jar")

    cases = get_repo_cases(_repos_path)
    for repo_case in cases:
        print(f"repo_case: {repo_case}")
        dsl_case = get_pattern_path(repo_case, _query_base_path)
        result_path = get_result_path(repo_case, _results_path)

        if not dsl_case.exists():
            print(f"Invalid query path: {dsl_case}")
            continue

        repo_path = next(repos_path.iterdir(), None)
        if repo_path is None:
            print("No repo found")
            continue

        for index, java_file in enumerate(repo_path.rglob("*.java")):
            index_result_path = result_path / f"{index}_error_kirin"
            kirin_engine(30, engine_path, dsl_case, java_file, index_result_path)


def calculate_det_num(_results_path: Path):
    det_num_list = []

    print(f"Total det num: {sum(det_num_list)}")
    print(f"Average det num: {sum(det_num_list) / len(det_num_list)}")
    print(f"det nums: {det_num_list}")


if __name__ == '__main__':
    dataset_name = "codeql_sampled_v1"
    query_base_path = Path("D:/workspace/CodeNavi-Generation/07dsl/") / dataset_name

    repos_path = Path("/data/jiangjiajun/CodeNavi-DSL/data") / f"{dataset_name}_repos"

    results_path = Path(f"/data/jiangjiajun/CodeNavi-DSL/GenPat/repo_{dataset_name}")

    detect_repo(query_base_path, repos_path, results_path)
    # calculate_det_num(results_path)
