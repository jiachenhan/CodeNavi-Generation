from pathlib import Path
"""用于把之前跑实验随机选择的case固定下来"""


if __name__ == "__main__":
    for dataset_commits_name in ["codeql_v1_commits_ori", "codeql_v2_commits_ori",
                                 "pmd_v1_commits_ori", "pmd_v2_commits_ori"]:
        dataset_commits_path = Path(f"/data/jiangjiajun/DSL-AutoDebug/{dataset_commits_name}")
        fixed_info_path = Path(f"/data/jiangjiajun/DSL-AutoDebug/{dataset_commits_name}/fixed_scanned_case_info.json")

        fixed_scanned_dict = {}

        for checker_path in dataset_commits_path.iterdir():
            if not checker_path.is_dir():
                continue
            fixed_scanned_dict.setdefault(checker_path.stem, {})

            for group_path in checker_path.iterdir():
                for case_path in group_path.iterdir():
                    if not (case_path / "sat_warnings.json").exists():
                        # 选择特定的case
                        continue
                    fixed_scanned_dict[checker_path.stem][group_path.stem] = case_path.stem

        if not fixed_info_path.exists():
            fixed_info_path.parent.mkdir(parents=True, exist_ok=True)
        with open(fixed_info_path, 'w', encoding="utf-8") as f:
            import json
            json.dump(fixed_scanned_dict, f, indent=4, ensure_ascii=False)
