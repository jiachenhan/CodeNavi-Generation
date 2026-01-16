"""
run_visualization.py

启动一个本地 HTTP 服务，并在启动前自动从指定结果目录生成 data.json + dashboard.html。
所有配置通过文件顶部的常量进行修改，不再依赖命令行参数。
"""

from __future__ import annotations

import http.server
import socketserver
from pathlib import Path

# 支持两种导入方式：直接运行（相对导入）或作为模块运行（绝对导入）
try:
    from dashboard_builder import prepare_dashboard, get_output_dir
except ImportError:
    from exp.thesis.visualization_ori.dashboard_builder import prepare_dashboard, get_output_dir


# === 可配置参数（按需修改） ===
# 存放检测结果的根目录： base_dir / {dataset} / {checker} / {group} / *_labeled_results.json
BASE_DIR = Path("E:/dataset/Navi/final_thesis_datas/ori_dsl_detect_results")

# 存放 DSL (.kirin) 文件的根目录： dsl_base_dir / {dataset} / {checker} / {group} / {case_num}.kirin
DSL_BASE_DIR = Path("E:/dataset/Navi/final_thesis_datas/ori_dsl")

# 存放代码对和 info.json 的数据集根目录： code_pair_base_dir / {mapped_dataset} / {checker} / {group} / {case1} / {buggy.java, fixed.java, info.json}
# 注意：这是构建的名为 DEFS 的数据集，mapped_dataset 为 ql (对应 codeql) 或 pmd
CODE_PAIR_BASE_DIR = Path("E:/dataset/Navi/DEFs")

# 可视化代码所在目录（包含 templates/ 与 static/）
WORK_DIR = Path(__file__).parent

# HTTP 服务端口
PORT = 8000


def make_handler(directory: Path):
    """为指定静态目录创建一个 HTTP 处理器类。"""

    class MyHandler(http.server.SimpleHTTPRequestHandler):
        def __init__(self, *args, **kwargs):
            super().__init__(*args, directory=str(directory), **kwargs)

    return MyHandler


def run_server() -> None:
    base_dir = BASE_DIR.resolve()
    dsl_base_dir = DSL_BASE_DIR.resolve()
    code_pair_base_dir = CODE_PAIR_BASE_DIR.resolve()
    work_dir = WORK_DIR.resolve()
    port = PORT
    output_dir = get_output_dir(work_dir)

    print(f"📦 结果根目录: {base_dir}")
    print(f"🧾 DSL 代码目录: {dsl_base_dir}")
    print(f"📝 代码对数据集目录: {code_pair_base_dir}")
    print(f"📁 可视化工作目录: {work_dir}")
    print(f"📂 输出目录: {output_dir}")
    print(f"🌐 服务端口: {port}")

    # 启动前准备数据与页面（会在 output_dir 写入 data.json 与 dashboard.html）
    prepare_dashboard(base_dir, work_dir, dsl_base_dir=dsl_base_dir, code_pair_base_dir=code_pair_base_dir)

    # 静态根目录使用 work_dir，这样既能访问 output/dashboard.html，也能访问 static/ 资源
    handler_cls = make_handler(work_dir)

    with socketserver.TCPServer(("", port), handler_cls) as httpd:
        print(f"🚀 服务器已启动: http://localhost:{port}/output/dashboard.html")
        print(f"📂 正在提供静态文件目录: {work_dir}")
        httpd.serve_forever()


if __name__ == "__main__":
    run_server()
