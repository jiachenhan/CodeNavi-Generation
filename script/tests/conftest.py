"""
pytest 配置文件

包含共享的 fixture 和测试配置。
"""
import sys
from pathlib import Path
from typing import Optional
import pytest

# 添加项目根目录到路径
project_root = Path(__file__).parent.parent
if str(project_root) not in sys.path:
    sys.path.insert(0, str(project_root))

# 检查是否在 uv 环境中运行
# 如果 antlr4 不可用，提示用户使用 uv run pytest
try:
    import antlr4
except ImportError:
    import warnings
    warnings.warn(
        "antlr4-python3-runtime not found in current Python environment. "
        "Please run tests using: uv run pytest\n"
        "Or install dependencies first: uv sync",
        ImportWarning
    )


# ============================================================================
# 测试数据路径配置
# ============================================================================
# 所有测试数据路径都应该通过这里统一管理
# 可以通过环境变量覆盖默认值

class TestDataPaths:
    """
    测试数据路径配置类
    
    统一管理所有测试数据路径，避免硬编码分散在各个测试文件中。
    
    修改路径配置：
        直接修改下面的类属性即可，所有测试都会使用新的路径。
    """
    
    # ========================================================================
    # 路径配置 - 直接在这里修改所有路径
    # ========================================================================
    
    # 数据集基础路径
    DATASET_BASE_PATH = Path("E:/dataset/Navi")
    
    # DSL 文件根目录（.kirin 文件）
    DSL_ROOT_PATH = Path("E:/dataset/Navi/final_thesis_datas/ori_dsl")
    
    # 检测结果文件根目录
    DETECT_RESULTS_ROOT_PATH = Path("E:/dataset/Navi/final_thesis_datas/ori_dsl_detect_results")
    
    # 缺陷修复数据根目录（DEFs）
    DEFS_ROOT_PATH = Path("E:/dataset/Navi/DEFs")

    # Refine 测试输出目录
    REFINE_OUTPUT_DIR = Path("E:/dataset/Navi/final_thesis_datas/refine_output_dsl")
    
    def __init__(self):
        """初始化测试数据路径配置"""
        pass
    
    @property
    def dataset_base_path(self) -> Path:
        """数据集基础路径"""
        return self.DATASET_BASE_PATH
    
    @property
    def dsl_root_path(self) -> Path:
        """DSL 文件根目录"""
        return self.DSL_ROOT_PATH
    
    @property
    def detect_results_root_path(self) -> Path:
        """检测结果文件根目录"""
        return self.DETECT_RESULTS_ROOT_PATH
    
    @property
    def defs_root_path(self) -> Path:
        """缺陷修复数据根目录"""
        return self.DEFS_ROOT_PATH

    @property
    def refine_output_dir(self) -> Path:
        """Refine 测试输出目录"""
        return self.REFINE_OUTPUT_DIR


# 全局测试数据路径配置实例
_test_data_paths = TestDataPaths()


@pytest.fixture(scope="session")
def test_data_paths() -> TestDataPaths:
    """
    测试数据路径配置 fixture（session 级别）
    
    所有测试都可以使用这个 fixture 来获取统一的测试数据路径。
    
    示例：
        ```python
        def test_something(test_data_paths):
            kirin_file = test_data_paths.dsl_root_path / "pmd_v1_commits" / "pattern" / "1.kirin"
            buggy_file = test_data_paths.defs_root_path / "pmd" / "pattern" / "1" / "buggy.java"
        ```
    """
    return _test_data_paths


@pytest.fixture(scope="session")
def dsl_root_path(test_data_paths: TestDataPaths) -> Path:
    """
    DSL 文件根目录 fixture（session 级别）
    
    用于需要访问 .kirin 文件根目录的测试。
    """
    return test_data_paths.dsl_root_path


@pytest.fixture(scope="session")
def dataset_base_path(test_data_paths: TestDataPaths) -> Path:
    """
    数据集基础路径 fixture（session 级别）
    
    用于需要访问数据集基础目录的测试。
    """
    return test_data_paths.dataset_base_path

@pytest.fixture(scope="session")
def refine_output_dir(test_data_paths: TestDataPaths) -> Path:
    """
    Refine 测试输出目录 fixture（session 级别）
    
    """
    return test_data_paths.refine_output_dir