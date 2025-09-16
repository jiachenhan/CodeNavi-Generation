from pathlib import Path
from typing import List, Generator, Tuple
import pandas as pd
import matplotlib.pyplot as plt
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
    """返回 (sub_dataset_name, group_id, num_cases) 列表"""
    results = []
    for checker_path in sub_dataset_path.iterdir():
        if checker_path.is_dir():
            for group_path in checker_path.iterdir():
                if group_path.is_dir():
                    num_cases = sum(1 for _ in group_path.iterdir())
                    group_id = f"{checker_path.stem}/{group_path.name}"
                    results.append((checker_path.parent.stem, group_id, num_cases))
    return results


def statistic():
    config = set_config("yunwu")
    jar_path = config.get("jar_path")
    dataset_path = Path("/Users/jiachenhan/DataSets/DEFs")
    results_path = Path("/Users/jiachenhan/DataSets/DEFss")
    stats_result = results_path / "stats.csv"

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

    # Group -> number of cases
    df_group = pd.DataFrame(group_data_all, columns=["dataset_name", "group_id", "num_cases"])
    df_group.to_csv(results_path / "group_case_distribution.csv", index=False)


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
    result_base = Path("/Users/jiachenhan/DataSets/DEFss")
    checker_summary_result = result_base / "checker_summary.csv"
    group_case_distribution_result = result_base / "group_case_distribution.csv"
    stats_result = result_base / "stats.csv"


    print("===== Pattern Edit & Line Change Stats =====")
    df = pd.read_csv(stats_result)

    print(f"Total cases: {len(df)}")
    print(f"Avg lines changed: {df['total_changed'].mean():.2f}")
    print(f"  Std: {df['total_changed'].std():.2f}, Max: {df['total_changed'].max()}, Min: {df['total_changed'].min()}, Median: {df['total_changed'].median()}")

    # ==== 分桶统计 ====
    bins = [0, 5, 10, 20, float("inf")]
    labels = ["≤5", "6–10", "11–20", ">20"]

    df["edit_size_bin"] = pd.cut(df["total_changed"], bins=bins, labels=labels, right=True)

    bucket_counts = df["edit_size_bin"].value_counts().reindex(labels, fill_value=0)
    bucket_perc   = (bucket_counts / len(df) * 100).round(1)

    print("\nEdit size distribution:")
    for label in labels:
        print(f"  {label}: {bucket_counts[label]} cases ({bucket_perc[label]}%)")

    # ==== 节点修改 ====
    print(f"\nAvg total edits: {df['total_edits'].mean():.2f}")
    for col in ['insert', 'delete', 'update', 'move']:
        if col in df.columns:
            print(f"  Avg {col}: {df[col].mean():.2f} (Std: {df[col].std():.2f})")


def sta_distribute():
    # === 输入 ===
    result_base = Path("/Users/jiachenhan/DataSets/DEFss")
    PATH_TO_CSV = result_base / "group_case_distribution.csv"
    TOP_K = 5

    # === 读取与解析 ===
    df = pd.read_csv(PATH_TO_CSV)
    assert "group_id" in df.columns and "num_cases" in df.columns and "dataset_name" in df.columns

    if "checker_name" not in df.columns or "cluster_id" not in df.columns:
        # 解析 checker_name 和 cluster_id
        split_cols = df["group_id"].astype(str).str.split("/", n=1, expand=True)
        df["checker_name"] = split_cols[0]
        df["cluster_id"]   = split_cols[1]

    # 规范化 dataset_name → 友好名称
    tool_map = {"ql": "CodeQL", "pmd": "PMD"}
    df["tool"] = df["dataset_name"].map(tool_map).fillna(df["dataset_name"])

    # === 1) 整体概览 ===
    total_clusters = len(df)
    avg_cases = df["num_cases"].mean()
    std_cases = df["num_cases"].std()
    min_cases = df["num_cases"].min()
    max_cases = df["num_cases"].max()
    median_cases = df["num_cases"].median()

    print("\n===== Dataset Overview (clusters as units) =====")
    print(f"Total clusters: {total_clusters}")
    print(f"Avg pairs/cluster: {avg_cases:.2f}")
    print(f"Std: {std_cases:.2f}, Min: {min_cases}, Median: {median_cases}, Max: {max_cases}")

    # === 2) Cluster size distribution ===
    # 典型分桶： [1], [2-3], [4-5], [6-10], [>10]
    bins = [1, 2, 4, 6, 11, float("inf")]   # 左闭右开
    labels = ["1", "2-3", "4-5", "6-10", ">10"]
    df["size_bin"] = pd.cut(df["num_cases"], bins=bins, labels=labels, right=False, include_lowest=True)

    dist_counts = df["size_bin"].value_counts().sort_index()
    dist_pct = (dist_counts / total_clusters * 100).round(1)

    dist_table = pd.DataFrame({
        "cluster_size_bin": labels,
        "#clusters": [dist_counts.get(l, 0) for l in labels],
        "%clusters": [dist_pct.get(l, 0.0) for l in labels],
    })
    print("\n===== Cluster Size Distribution =====")
    print(dist_table.to_string(index=False))

    # 可选：导出给论文作图/表用
    dist_table.to_csv(result_base / "appendix_cluster_size_distribution.csv", index=False)

    # === 3) Checker 维度的聚合统计 ===
    # 按工具与 checker 聚合
    per_checker = df.groupby(["tool", "checker_name"]).agg(
        num_clusters=("cluster_id", "nunique"),
        num_pairs=("num_cases", "sum"),
        avg_pairs_per_cluster=("num_cases", "mean"),
    ).reset_index()
    per_checker["avg_pairs_per_cluster"] = per_checker["avg_pairs_per_cluster"].round(2)

    def dump_top_k(tool_name: str, out_csv: Path):
        d = per_checker[per_checker["tool"] == tool_name].copy()
        if d.empty:
            print(f"[WARN] No rows for tool='{tool_name}'")
            return
        d = d.sort_values(["num_clusters", "num_pairs"], ascending=False).head(TOP_K)
        pretty = d.rename(columns={
            "checker_name": "Checker / Defect Type",
            "num_clusters": "#Clusters",
            "num_pairs": "#Pairs",
            "avg_pairs_per_cluster": "Avg Pairs/Cluster",
        })[["Checker / Defect Type", "#Clusters", "#Pairs", "Avg Pairs/Cluster"]]
        pretty.to_csv(out_csv, index=False)
        print(f"\n===== Top-{TOP_K} {tool_name} checkers =====")
        print(pretty.to_string(index=False))

    dump_top_k("CodeQL", result_base / "appendix_top_codeql_checkers.csv")
    dump_top_k("PMD",    result_base / "appendix_top_pmd_checkers.csv")


    # === 4) 给论文用的汇总句（把数字带入正文/附录） ===
    # 你也可以把这些 f-string 的输出复制到论文中。
    print("\n===== Sentences for Paper (you can paste) =====")
    print(
        f"The dataset contains {total_clusters} clusters (avg {avg_cases:.2f} pairs/cluster; "
        f"median {median_cases}; min {min_cases}; max {max_cases}). "
        f"Cluster sizes are distributed as: " +
        ", ".join([f"{row['cluster_size_bin']}: {row['%clusters']}%" for _, row in dist_table.iterrows()]) + "."
    )


def plot_cluster_size_distribution_pretty(csv_path: Path,
                                          out_pdf: str = "cluster_size_distribution.pdf",
                                          out_png: str = "cluster_size_distribution.png",
                                          use_percentage: bool = False):
    summary_csv = "/Users/jiachenhan/DataSets/DEFss/appendix_cluster_size_distribution.csv"
    df = pd.read_csv(summary_csv)

    # 丢弃不可能出现的分桶：“1”
    df = df[df["cluster_size_bin"] != "1"].copy()

    # 设定分桶显示顺序
    order = ["2-3", "4-5", "6-10", ">10"]
    df["cluster_size_bin"] = pd.Categorical(df["cluster_size_bin"], categories=order, ordered=True)
    df = df.sort_values("cluster_size_bin")

    # 使用百分比作为 y 值（论文更直观）
    x = df["cluster_size_bin"].astype(str)  # 转成字符串
    y_pct = df["%clusters"].astype(float)

    # ========= 论文友好风格设置（Matplotlib）=========
    plt.rcParams.update({
        "font.family": "serif",
        "font.serif": ["Times New Roman", "Times", "DejaVu Serif"],
        "axes.labelsize": 11,
        "axes.titlesize": 12,
        "xtick.labelsize": 10,
        "ytick.labelsize": 10,
    })

    fig, ax = plt.subplots(figsize=(4.0, 3.0))  # 单栏友好尺寸

    bars = ax.bar(x, y_pct)  # 使用默认配色，避免喧宾夺主

    # 只保留下/左坐标轴，隐藏上/右
    ax.spines["top"].set_visible(False)
    ax.spines["right"].set_visible(False)
    ax.spines["left"].set_linewidth(0.8)
    ax.spines["bottom"].set_linewidth(0.8)

    # 细灰水平网格线，便于读数
    ax.yaxis.grid(True, linestyle="--", linewidth=0.6, alpha=0.5)
    ax.xaxis.grid(False)

    # 轴标签与标题
    ax.set_xlabel("Pairs per cluster")
    ax.set_ylabel("%Clusters")
    ax.set_title("Cluster size distribution")

    # y 轴范围略留顶部空间，给标注让位
    ymax = max(y_pct.max() * 1.15, y_pct.max() + 3)
    ax.set_ylim(0, ymax)

    # 柱顶标注：百分比（保留 1 位小数）
    ax.bar_label(bars, labels=[f"{v:.1f}%" for v in y_pct], padding=2, fontsize=9)

    plt.tight_layout()

    # 保存（沿用你原来的输出路径名）
    fig.savefig("cluster_size_distribution.pdf", bbox_inches="tight")
    fig.savefig("cluster_size_distribution.png", dpi=300, bbox_inches="tight")
    plt.close(fig)


def main():
    # statistic()
    show_statistic()
    # sta_distribute()
    # plot_cluster_size_distribution_pretty(Path("/Users/jiachenhan/DataSets/DEFss/appendix_cluster_size_distribution.csv"))

if __name__ == "__main__":
    main()