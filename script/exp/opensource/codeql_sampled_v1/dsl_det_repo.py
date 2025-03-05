import platform
import stat
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

    cases = get_repo_cases(_repos_path)
    for repo_case in cases:
        print(f"repo_case: {repo_case}")
        dsl_case = get_pattern_path(repo_case, _query_base_path)
        result_path = get_result_path(repo_case, _results_path)

        if result_path.exists():
            continue

        if not dsl_case.exists():
            print(f"Invalid query path: {dsl_case}")
            continue

        repo_path = next(repo_case.iterdir(), None)
        if repo_path is None:
            print("No repo found")
            continue

        for index, pkg in enumerate(split_java_package(repo_path)):
            index_result_path = result_path / f"{index}_error_kirin"
            timeout = kirin_engine(60, engine_path, dsl_case, pkg, index_result_path)
            if timeout:
                print(f"Timeout in : {dsl_case}")
                break


def calculate_det_num(_results_path: Path):
    det_num_list = []
    for checker in _results_path.iterdir():
        for group in checker.iterdir():
            for case in group.iterdir():
                print(f"case: {case}")
                all_count = 0
                for _slice in case.iterdir():
                    report_file = _slice / "error_report_1.xml"
                    all_count += xml_count_errors(report_file)

                det_num_list.append((case, all_count))

    total_det_nums = sum([det_num for _, det_num in det_num_list])
    print(f"Total det num: {total_det_nums}")
    print(f"Average det num: {total_det_nums / len(det_num_list)}")
    print("det nums:")
    for _ in det_num_list:
        print(f"{_[0]}: {_[1]}")


def xml_count_errors(_output_path: Path) -> int:
    if not _output_path.exists():
        return 0

    import xml.etree.ElementTree as ET
    xml_root = ET.parse(_output_path)
    all_error = xml_root.find("errors").findall("error")
    return len(all_error)


if __name__ == '__main__':
    dataset_name = "codeql_sampled_v1"
    query_base_path = Path("D:/workspace/CodeNavi-Generation/07dsl/") / dataset_name

    repos_path = Path("/data/jiangjiajun/CodeNavi-DSL/data") / f"{dataset_name}_repos"

    results_path = Path(f"/data/jiangjiajun/CodeNavi-DSL/GenPat/repo_{dataset_name}")

    # detect_repo(query_base_path, repos_path, results_path)
    calculate_det_num(results_path)
