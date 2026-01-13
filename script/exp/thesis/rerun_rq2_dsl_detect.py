import json
from pathlib import Path
from typing import Optional

from interface.java.run_java_api import kirin_engine

dataset_names = ["codeql_sampled_v1", "codeql_sampled_v2", "pmd_sampled_v1", "pmd_sampled_v2"]


def _rerun_detect_on_commit(_query_path: Path,
                            _commit_repo_path: Path,
                            _result_path: Path,
                            _engine_path: Path
                            ):
    timeout = kirin_engine(3600, _engine_path, _query_path, _commit_repo_path, _result_path)
    if timeout:
        print(f"Timeout in : {_query_path}")


def _get_fixed_info(_fixed_info_path: Path) -> dict:
    with open(_fixed_info_path, 'r', encoding="utf-8") as f:
        fixed_info = json.load(f)
        return fixed_info


def _get_scanned_case(_fixed_info: dict,
                      _checker_name: str,
                      _group_name: str
                      ) -> Optional[str]:
    return _fixed_info.get(_checker_name, {}).get(_group_name, None)


def main():
    engine_path = Path("D:/envs/kirin-cli-1.0.8_sp06-jackofext-obfuscate.jar")

    query_base_path = Path(f"D:/workspace/CodeNavi-Generation/07dsl/3-21-all-sampled")

    commit_datasets_path = Path(f"E:/dataset/Navi/rq2_commit")
    results_base_path = Path(f"E:/dataset/Navi/final_thesis_datas/ori_dsl_detect_results")
    for dataset in commit_datasets_path.iterdir():
        fixed_info_path = dataset / "fixed_scanned_case_info.json"
        fixed_infos = _get_fixed_info(fixed_info_path)

        for checker_path in dataset.iterdir():
            if not checker_path.is_dir():
                continue

            for group_path in checker_path.iterdir():
                if not group_path.is_dir():
                    continue

                def transform_dataset_name(s):
                    match s.split('_'):
                        case [tool, version, "commits"] if version in ["v1", "v2"] and tool in ["codeql", "pmd"]:
                            return f"{tool}_sampled_{version}"
                        case _:
                            raise ValueError(f"Unexpected dataset name format: {s}")
                query_path = next((query_base_path / transform_dataset_name(dataset.stem)
                                   / checker_path.stem / group_path.stem).glob("[0-9]*.kirin"), None)
                if query_path is None:
                    print(f"No query found for {checker_path.stem}/{group_path.stem}")
                    continue

                scanned_case_name = _get_scanned_case(fixed_infos, checker_path.stem, group_path.stem)
                if scanned_case_name is None:
                    print(f"No scanned case found for {checker_path.stem}/{group_path.stem}")
                    continue

                scanned_case_path = group_path / scanned_case_name

                results_path = (results_base_path / dataset.stem / checker_path.stem / group_path.stem /
                                f"{query_path.stem}_{scanned_case_name}")
                if not results_path.exists():
                    results_path.mkdir(parents=True, exist_ok=True)
                else:
                    print(f"Result path exists, skip: {results_path}")
                    continue

                _rerun_detect_on_commit(query_path, scanned_case_path / "after", results_path, engine_path)
    pass


if __name__ == "__main__":
    main()
