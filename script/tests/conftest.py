"""
pytest 配置文件

包含共享的 fixture 和测试配置。
"""
import sys
from pathlib import Path

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
