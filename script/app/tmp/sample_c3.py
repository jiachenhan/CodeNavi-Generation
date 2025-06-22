import math
import random
import shutil
from functools import reduce
from operator import add
from pathlib import Path

from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def list_groups(path: Path):
    """列出指定路径下的所有子目录"""
    return [d for d in path.iterdir() if d.is_dir()]


def total_groups(project_groups: dict):
    """计算所有项目的总目录数"""
    nested_list = project_groups.values()
    return reduce(add, (len(sublist) for sublist in nested_list), 0)


def random_select_groups(directories, num_to_select):
    """从目录列表中随机选择若干个目录"""
    random.seed(42)
    return random.sample(directories, num_to_select)


def copy_groups(source_base_path, target_base_path, directories: list[Path]):
    target_base_path.mkdir(parents=True, exist_ok=True)
    """将选中的目录复制到目标路径"""
    for directory in directories:
        source_path = source_base_path / directory.name
        target_path = target_base_path / directory.name
        _logger.info(f"Copy {source_path} to {target_path}")
        shutil.copytree(source_path, target_path)


def main():
    c3_path = Path("/data/jiangjiajun/LLMPAT/temporary/processed_data")
    c3_sample_path = Path("/data/jiangjiajun/LLMPAT/dataset/c3_random_1000")
    ramdom_size = 1000
    project_groups = {}
    for project in c3_path.iterdir():
        groups = list_groups(project)
        project_groups[project] = groups

    for project, groups in project_groups.items():
        ratio = ramdom_size / total_groups(project_groups)
        selected_size = math.ceil(ratio * len(groups))
        _logger.info(f"{project}: {selected_size}")
        selected_groups = random_select_groups(groups, selected_size)
        target_project_path = c3_sample_path / project.name
        copy_groups(project, target_project_path, selected_groups)


def count_c3_pairs():
    c3_sample_path = Path("/data/jiangjiajun/CodeNavi-DSL/data/c3_sampled_v1")
    total_pairs = sum(1 for _ in c3_sample_path.rglob("before.java"))
    print(total_pairs)


if __name__ == "__main__":
    # main()
    count_c3_pairs()
