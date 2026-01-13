from pathlib import Path
from typing import List, Generator, Tuple
import pandas as pd
from pathlib import Path

from interface.java.run_java_api import statistic_dataset
from utils.config import set_config


def get_group_size(dataset_path: Path):
    """返回 (group_name, num_groups) 列表"""
    results = []
    for sub_dataset in dataset_path.iterdir():
        if sub_dataset.is_dir():
            sub_dataset_name = sub_dataset.stem
            num_groups = sum(1 for _ in sub_dataset.iterdir() if _.is_dir())
            results.append((sub_dataset_name, num_groups))
    return results

def get_cases_num(dataset_path: Path):
    """返回 (group_id, num_cases) 列表"""
    results = []
    for sub_dataset_path in dataset_path.iterdir():
        if sub_dataset_path.is_dir():
            for group_path in sub_dataset_path.iterdir():
                if group_path.is_dir():
                    num_cases = sum(1 for _ in group_path.iterdir())
                    group_id = f"{sub_dataset_path.stem}/{group_path.name}"
                    results.append((group_id, num_cases))
    return results

def write_summary_csvs(checker_data: list, group_data: list, result_base_path: Path):
    # sub_dataset -> number of groups
    df_checker = pd.DataFrame(checker_data, columns=["sub_dataset", "num_groups"])
    df_checker.to_csv(result_base_path / "sub_dataset_summary.csv", index=False)

    # Group -> number of cases
    df_group = pd.DataFrame(group_data, columns=["group_id", "num_cases"])
    df_group.to_csv(result_base_path / "group_case_distribution.csv", index=False)


def statistic():
    config = set_config("yunwu")
    jar_path = config.get("jar_path")
    dataset_path = Path("E:/dataset/Navi/c3_sampled_v1")
    stats_result = Path("E:/dataset/Navi/c3_sampled_v1s/stats.csv")

    stats_result.parent.mkdir(parents=True, exist_ok=True)
    checker_data_all = []
    group_data_all = []
    # 计算统计指标
    checker_data = get_group_size(dataset_path)
    checker_data_all.extend(checker_data)

    group_data = get_cases_num(dataset_path)
    group_data_all.extend(group_data)

    write_summary_csvs(checker_data_all, group_data_all, Path("E:/dataset/Navi/c3_sampled_v1s"))


    # # 计算改动指标
    # all_stats = []
    # for sub_dataset in dataset_path.iterdir():
    #     if not sub_dataset.is_dir():
    #         continue
    #     for buggy_path in sub_dataset.rglob("buggy.java"):
    #         fixed_path = buggy_path.parent / "fixed.java"
    #         stats = statistic_dataset(30, buggy_path, fixed_path, jar_path)
    #         stats['dataset'] = sub_dataset.name
    #         stats['case'] = str(buggy_path.parent)
    #         all_stats.append(stats)
    #
    # df = pd.DataFrame(all_stats)
    # stats_result.parent.mkdir(parents=True, exist_ok=True)
    # df.to_csv(stats_result, index=False)


def show_statistic():
    result_base = Path("E:/dataset/Navi/c3_sampled_v1s")
    sub_dataset_summary_result = result_base / "sub_dataset_summary.csv"
    group_case_distribution_result = result_base / "group_case_distribution.csv"
    stats_result = result_base / "stats.csv"


    # print("===== Pattern Edit & Line Change Stats =====")
    # df = pd.read_csv(stats_result)
    #
    # print(f"Total cases: {len(df)}")
    # print(f"Avg lines changed: {df['total_changed'].mean():.2f}")
    # print(f"  Std: {df['total_changed'].std():.2f}, Max: {df['total_changed'].max()}, Min: {df['total_changed'].min()}, Median: {df['total_changed'].median()}")
    #
    # print(f"\nAvg total edits: {df['total_edits'].mean():.2f}")
    # for col in ['insert', 'delete', 'update', 'move']:
    #     if col in df.columns:
    #         print(f"  Avg {col}: {df[col].mean():.2f} (Std: {df[col].std():.2f})")

    print("\n===== Group-Level Case Distribution =====")
    df_cases = pd.read_csv(group_case_distribution_result)
    print(f"Total groups: {len(df_cases)}")
    print(f"Avg cases per group: {df_cases['num_cases'].mean():.2f}")
    print(f"  Std: {df_cases['num_cases'].std():.2f}, Max: {df_cases['num_cases'].max()}, Min: {df_cases['num_cases'].min()}, Median: {df_cases['num_cases'].median()}")

    print("\n===== SubDataset Size =====")
    df_groups = pd.read_csv(sub_dataset_summary_result)
    print(f"Total Groups: {len(df_groups)}")


def main():
    statistic()
    show_statistic()


if __name__ == "__main__":
    main()