"""
generate_dashboard.py

保留一个独立脚本，方便在命令行下单独生成 dashboard（不启动服务）。
真实的数据处理和渲染逻辑在 core.py 中实现。
"""

from pathlib import Path

from dashboard_builder import merge_results_from_dirs, generate_dashboard


if __name__ == "__main__":
    # TODO: 根据自己的实验目录调整默认路径
    base_dir = Path("E:/dataset/Navi/final_thesis_datas/ori_dsl_detect_results")
    dsl_base_dir = Path("E:/dataset/Navi/final_thesis_datas/ori_dsl")
    work_dir = Path(__file__).parent

    data = merge_results_from_dirs(base_dir, dsl_base_dir=dsl_base_dir)
    generate_dashboard(data, work_dir)
