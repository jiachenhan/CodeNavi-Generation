import json
from pathlib import Path

import numpy as np
import seaborn as sns
import matplotlib.pyplot as plt
import pandas as pd


def filter_outliers(series):
    q1 = series.quantile(0.25)
    q3 = series.quantile(0.75)
    iqr = q3 - q1
    return series.between(q1-1.5*iqr, q3+1.5*iqr)

def view_result_box(df, _store_path: Path):
    sns.boxplot(data=df)
    plt.xlabel("methods", fontsize=12)
    plt.ylabel("detect num", fontsize=12)
    plt.xticks(rotation=45)  # 避免长方法名重叠[4](@ref)
    plt.savefig(_store_path, dpi=600)

def view_result_vio(df, _store_path: Path):
    cleaned_long = df.melt(var_name='method', value_name='detect_num')  # 宽格式转长格式

    sns.violinplot(
        x="method",
        y="detect_num",
        data=cleaned_long,
        scale="count",    # 宽度反映样本数量[6,8](@ref)
        inner="quartiles",# 显示四分位数线[1](@ref)
        palette="Set3",   # 颜色方案[3](@ref)
        bw=0.2,           # 核密度估计带宽[1](@ref)
        linewidth=1       # 轮廓线粗细[6](@ref)
    )

    plt.xlabel("methods", fontsize=12)
    plt.ylabel("detect num", fontsize=12)
    plt.xticks(rotation=45)  # 避免长方法名重叠[4](@ref)
    plt.savefig(_store_path, dpi=600)


def view_result_histogram(df, _store_path: Path):
    plt.figure(figsize=(10, 6))  # 调整画布大小[3](@ref)
    # 遍历DataFrame的每一列（每个数据集）
    for column in df.columns:
        plt.hist(
            df[column],
            bins=30,           # 分箱数，根据数据范围调整[3,4](@ref)
            alpha=0.5,         # 透明度（0.3-0.7较合适）[3,4](@ref)
            label=column,      # 图例标签
            edgecolor='black'  # 直方图边框颜色
        )

    plt.xlabel("Methods", fontsize=12)
    plt.ylabel("Frequency", fontsize=12)
    plt.xticks(rotation=45)    # 保持方法名旋转
    plt.legend()               # 显示图例[3,4](@ref)
    plt.grid(True, alpha=0.3)  # 添加网格线[3](@ref)
    plt.savefig(_store_path, dpi=600, bbox_inches='tight')


def view_result(_result_path: Path, _store_path: Path):
    _results = json.load(open(_result_path, "r", encoding="utf-8"))

    data = [item.get("result").get("all_scanned") for item in _results]
    datas = {
        "Genpat": data
    }
    df = pd.DataFrame(datas)
    cleaned_df = df[df.apply(filter_outliers).all(axis=1)]

    # view_result_box(cleaned_df, _store_path)
    view_result_histogram(cleaned_df, _store_path)


if __name__ == "__main__":
    result_path = Path("E:/dataset/Navi/result_trans_repo_codeql_sampled_v1/navi_result_store.json")
    store_path = Path("E:/dataset/Navi/result_trans_repo_codeql_sampled_v1/det_num_his.png")
    view_result(result_path, store_path)


