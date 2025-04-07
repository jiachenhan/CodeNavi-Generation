import json
import platform
import random
import stat
from pathlib import Path
from typing import Generator

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
    return _query_path.rglob("*.kirin")


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

    for _scanned_repo_path in _repos_path.iterdir():
        for _dsl_path in get_dsl_paths(_query_base_path):
            _result_path = _results_path / _scanned_repo_path.stem / _dsl_path.stem

            for index, pkg in enumerate(split_java_package(_scanned_repo_path)):
                index_result_path = _result_path / f"{index}_error_kirin"
                timeout = kirin_engine(300, engine_path, _dsl_path, pkg, index_result_path)
                if timeout:
                    print(f"Timeout in : {_dsl_path}")


if __name__ == '__main__':
    dataset_name = "vul4j"
    query_base_path = Path(f"E:/dataset/Navi/vul/ql/{dataset_name}")

    repos_path = Path(f"E:/dataset/Navi/local_repos")
    results_path = Path(f"E:/dataset/Navi/vul/results/{dataset_name}_2")

    detect_repo(query_base_path, repos_path, results_path)
