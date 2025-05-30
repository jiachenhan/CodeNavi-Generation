"""
    codenavi unsupport src/test dir , rename them to untest
"""
from pathlib import Path


def rename_src_test_file(root_path: Path, new_name: str = "untest"):
    root = Path(root_path)
    # 查找所有名为'src'的目录
    for src_dir in root.rglob('src'):
        if src_dir.is_dir():
            test_dir = src_dir / 'test'
            # 检查test目录是否存在并且是一个目录
            if test_dir.exists() and test_dir.is_dir():
                untest_dir = src_dir / 'untest'
                if untest_dir.exists():
                    print(f"目录已存在，跳过: {untest_dir}")
                else:
                    try:
                        test_dir.rename(untest_dir)
                        print(f"已将 '{test_dir}' 重命名为 '{untest_dir}'")
                    except OSError as e:
                        print(f"重命名失败: {test_dir} -> {untest_dir}, 错误: {e}")


if __name__ == "__main__":
    dataset_name = "pmd_sampled_v2"
    repos_path = Path(f"E:/dataset/Navi/{dataset_name}_repos")
    rename_src_test_file(repos_path)



