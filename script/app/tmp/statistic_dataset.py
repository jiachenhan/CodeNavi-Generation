from pathlib import Path
from typing import List, Generator, Tuple
import pandas as pd
from pathlib import Path

from interface.java.run_java_api import statistic_dataset
from utils.config import set_config

def get_checkers_diversity(sub_dataset_path: Path):
    """返回 (checker_name, num_groups) 列表"""
    results = []
    for checker_path in sub_dataset_path.iterdir():
        if checker_path.is_dir():
            checker_name = checker_path.stem
            num_groups = sum(1 for _ in checker_path.iterdir() if _.is_dir())
            results.append((checker_name, num_groups))
    return results

def get_cases_num(sub_dataset_path: Path):
    """返回 (group_id, num_cases) 列表"""
    results = []
    for checker_path in sub_dataset_path.iterdir():
        if checker_path.is_dir():
            for group_path in checker_path.iterdir():
                if group_path.is_dir():
                    num_cases = sum(1 for _ in group_path.iterdir())
                    group_id = f"{checker_path.stem}/{group_path.name}"
                    results.append((group_id, num_cases))
    return results

def write_summary_csvs(checker_data: list, group_data: list, result_base_path: Path):
    # Checker -> number of groups
    df_checker = pd.DataFrame(checker_data, columns=["checker", "num_groups"])
    df_checker.to_csv(result_base_path / "checker_summary.csv", index=False)

    # Group -> number of cases
    df_group = pd.DataFrame(group_data, columns=["group_id", "num_cases"])
    df_group.to_csv(result_base_path / "group_case_distribution.csv", index=False)


def statistic():
    config = set_config("yunwu")
    jar_path = config.get("jar_path")
    dataset_path = Path("/Users/jiachenhan/DataSets/DEFs")
    stats_result = Path("/Users/jiachenhan/DataSets/DEFss/stats.csv")

    stats_result.parent.mkdir(parents=True, exist_ok=True)
    checker_data_all = []
    group_data_all = []
    # 计算统计指标
    for sub_dataset in dataset_path.iterdir():
        if not sub_dataset.is_dir():
            continue
        checker_data = get_checkers_diversity(sub_dataset)
        checker_data_all.extend(checker_data)

        group_data = get_cases_num(sub_dataset)
        group_data_all.extend(group_data)

    write_summary_csvs(checker_data_all, group_data_all, Path("/Users/jiachenhan/DataSets/DEFss"))


    # 计算改动指标
    all_stats = []
    for sub_dataset in dataset_path.iterdir():
        if not sub_dataset.is_dir():
            continue
        for buggy_path in sub_dataset.rglob("buggy.java"):
            fixed_path = buggy_path.parent / "fixed.java"
            stats = statistic_dataset(30, buggy_path, fixed_path, jar_path)
            stats['dataset'] = sub_dataset.name
            stats['case'] = str(buggy_path.parent)
            all_stats.append(stats)

    df = pd.DataFrame(all_stats)
    stats_result.parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(stats_result, index=False)


def show_statistic():
    result_base = Path("/Users/jiachenhan/DataSets/DEFss")
    checker_summary_result = result_base / "checker_summary.csv"
    group_case_distribution_result = result_base / "group_case_distribution.csv"
    stats_result = result_base / "stats.csv"


    print("===== Pattern Edit & Line Change Stats =====")
    df = pd.read_csv(stats_result)

    print(f"Total cases: {len(df)}")
    print(f"Avg lines changed: {df['total_changed'].mean():.2f}")
    print(f"  Std: {df['total_changed'].std():.2f}, Max: {df['total_changed'].max()}, Min: {df['total_changed'].min()}, Median: {df['total_changed'].median()}")

    print(f"\nAvg total edits: {df['total_edits'].mean():.2f}")
    for col in ['insert', 'delete', 'update', 'move']:
        if col in df.columns:
            print(f"  Avg {col}: {df[col].mean():.2f} (Std: {df[col].std():.2f})")

    print("\n===== Group-Level Case Distribution =====")
    df_group = pd.read_csv(group_case_distribution_result)
    print(f"Total groups: {len(df_group)}")
    print(f"Avg cases per group: {df_group['num_cases'].mean():.2f}")
    print(f"  Std: {df_group['num_cases'].std():.2f}, Max: {df_group['num_cases'].max()}, Min: {df_group['num_cases'].min()}, Median: {df_group['num_cases'].median()}")

    print("\n===== Checker Diversity =====")
    df_checker = pd.read_csv(checker_summary_result)
    print(f"Total checkers: {len(df_checker)}")
    print("Top checkers by group count:")
    print(df_checker.sort_values("num_groups", ascending=False).head(5).to_string(index=False))


def main():
    # statistic()
    show_statistic()


if __name__ == "__main__":
    main()