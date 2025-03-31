import random
import shutil
from pathlib import Path
from typing import Generator, Tuple

from utils.common import reservoir_sampling


def get_all_groups(path: Path) -> Generator[Path, None, None]:
    for checker in path.iterdir():
        if not checker.is_dir():
            continue

        for group in checker.iterdir():
            if not group.is_dir():
                continue
            yield group

def copy_directory(src: Path, dst: Path):
    """
    复制目录内容（包含子目录）
    参数：
    src: Path - 源目录路径
    dst: Path - 目标目录路径
    """
    dst.mkdir(parents=True, exist_ok=True)
    shutil.copytree(src, dst, dirs_exist_ok=True)


def sample_code_ql():
    sample_num = 60
    data_base_path = Path("/data/jiangjiajun/DSL-AutoDebug/data/codeql_results_clustering_hierarchical")
    target_data_path = Path(f"/data/jiangjiajun/CodeNavi-DSL/data/codeql_hiera_{sample_num}")
    groups = get_all_groups(data_base_path)
    random_groups = reservoir_sampling(groups, sample_num)
    for index, group_path in enumerate(random_groups):
        print(f"{index}/{sample_num}: {group_path}")
        target_group_path = target_data_path / str(index)
        for case in group_path.iterdir():
            copy_directory(case, target_group_path / case.stem)


def sample_pmd():
    sample_num = 60
    data_base_path = Path("/data/jiangjiajun/DSL-AutoDebug/data/pmd_results_clustering_hierarchical")
    target_data_path = Path(f"/data/jiangjiajun/CodeNavi-DSL/data/pmd_hiera_{sample_num}")
    groups = get_all_groups(data_base_path)
    random_groups = reservoir_sampling(groups, sample_num)
    for index, group_path in enumerate(random_groups):
        print(f"{index}/{sample_num}: {group_path}")
        target_group_path = target_data_path / str(index)
        for case in group_path.iterdir():
            copy_directory(case, target_group_path / case.stem)


if __name__ == "__main__":
    sample_pmd()
    sample_code_ql()
