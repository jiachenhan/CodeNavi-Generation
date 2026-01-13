from __future__ import annotations

"""
dashboard_builder.py

封装可视化所需的数据合并与页面构建逻辑。
"""

import json
from pathlib import Path

from jinja2 import Environment, FileSystemLoader

OUTPUT_SUBDIR = "output"


def get_output_dir(work_dir: Path) -> Path:
    """返回用于存放生成文件的目录（例如 work_dir/output）。"""
    return work_dir.resolve() / OUTPUT_SUBDIR


def merge_results_from_dirs(
    base_dir: Path,
    dsl_base_dir: Path | None = None,
) -> list[dict]:
    """
    合并 base_dir 下所有数据集的标注结果，返回统一的列表。

    目录层级约定：
      base_dir / {dataset} / {checker} / {group} / *_labeled_results.json
    """
    results: list[dict] = []
    base_dir = base_dir.resolve()
    dsl_base_dir = dsl_base_dir.resolve() if dsl_base_dir is not None else None

    # 缓存 DSL 文件内容，避免重复读取
    dsl_cache: dict[Path, str] = {}

    for dataset_dir in base_dir.iterdir():
        if not dataset_dir.is_dir():
            continue
        for checker_dir in dataset_dir.iterdir():
            if not checker_dir.is_dir():
                continue
            for group_dir in checker_dir.iterdir():
                if not group_dir.is_dir():
                    continue
                labeled_files = list(group_dir.glob("*_labeled_results.json"))
                if not labeled_files:
                    continue

                for labeled_file in labeled_files:

                    def get_case_info(case_file_name: str) -> str:
                        parts = case_file_name.split("_")
                        if len(parts) == 4 and parts[2] == "labeled" and parts[3] == "results":
                            dsl_case, scanned_case, *_ = parts
                            return f"{dsl_case}_{scanned_case}"
                        raise ValueError(f"Unexpected dataset name format: {case_file_name}")

                    with labeled_file.open("r", encoding="utf-8") as f:
                        items = json.load(f)

                    # 每个 labeled_file 对应一个 caseInfo，可以用来推导 DSL 文件名
                    case_info = get_case_info(labeled_file.stem)
                    case_num = case_info.split("_", 1)[0]

                    dsl_source: str | None = None
                    if dsl_base_dir is not None:
                        dsl_file = (
                            dsl_base_dir
                            / dataset_dir.name
                            / checker_dir.name
                            / group_dir.name
                            / f"{case_num}.kirin"
                        )
                        if dsl_file in dsl_cache:
                            dsl_source = dsl_cache[dsl_file]
                        elif dsl_file.is_file():
                            with dsl_file.open("r", encoding="utf-8") as df:
                                dsl_source = df.read()
                            dsl_cache[dsl_file] = dsl_source

                    for item in items:
                        item["dataset"] = dataset_dir.name
                        item["checker"] = checker_dir.name
                        item["group"] = group_dir.name
                        item["case_info"] = case_info
                        if dsl_source is not None:
                            item["dsl_source"] = dsl_source
                        results.append(item)

    return results


def generate_dashboard(data: list[dict], work_dir: Path, output_html: Path | None = None) -> Path:
    """
    基于传入的数据生成 data.json 与 HTML 页面。

    - data.json 写在 output_dir / data.json
    - dashboard.html 默认写在 output_dir / dashboard.html，或使用 output_html 覆盖
    - 返回最终 HTML 的路径
    """
    work_dir = work_dir.resolve()
    output_dir = get_output_dir(work_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    templates_dir = work_dir / "templates"

    env = Environment(loader=FileSystemLoader(templates_dir))
    template = env.get_template("dashboard_template.html")

    # 写入 data.json
    data_file_path = output_dir / "data.json"
    with data_file_path.open("w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    # 渲染 HTML
    html_content = template.render()
    output_path = output_html or (output_dir / "dashboard.html")
    with output_path.open("w", encoding="utf-8") as f:
        f.write(html_content)

    print(f"✅ data.json 已生成: {data_file_path}")
    print(f"✅ 可视化页面已生成: {output_path}")
    return output_path


def prepare_dashboard(
    base_dir: Path,
    work_dir: Path,
    dsl_base_dir: Path | None = None,
) -> Path:
    """
    一步完成数据合并 + dashboard 生成，供服务启动时调用。

    返回生成的 HTML 路径。
    """
    base_dir = base_dir.resolve()
    work_dir = work_dir.resolve()

    print(f"🔎 正在从结果目录收集数据: {base_dir}")
    if dsl_base_dir is not None:
        print(f"🧾 DSL 代码目录: {dsl_base_dir}")
    data = merge_results_from_dirs(base_dir, dsl_base_dir=dsl_base_dir)
    print(f"📊 共收集到 {len(data)} 条函数级别记录")

    return generate_dashboard(data, work_dir)

