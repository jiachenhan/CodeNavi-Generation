# 测试说明

本目录包含所有测试代码，使用 pytest 框架。

## 目录结构

```
tests/
├── __init__.py
├── conftest.py          # pytest 配置和共享 fixture
├── parser/              # DSL 解析器测试
│   ├── __init__.py
│   ├── test_parser_all_kirin.py  # 批量测试 .kirin 文件解析
│   └── test_validator.py          # DSL 验证器测试
└── refine/              # DSL 优化框架测试
    ├── __init__.py
    └── test_refiner.py            # DSL 优化器测试
```

## 运行测试

### 运行所有测试

**推荐方式（使用 uv 环境）：**
```bash
cd script
uv run pytest
```

**或者（如果已激活 uv 环境）：**
```bash
cd script
pytest
```

**注意**：如果直接运行 `pytest` 时遇到 `ModuleNotFoundError: No module named 'antlr4'`，请使用 `uv run pytest` 来运行测试，这会确保使用正确的 Python 环境。

### 运行特定的测试包/目录

```bash
# 运行 parser 包下的所有测试
pytest tests/parser/

# 运行 refine 包下的所有测试
pytest tests/refine/

# 运行特定测试文件
pytest tests/parser/test_validator.py

# 运行特定测试类
pytest tests/parser/test_validator.py::TestDSLValidator

# 运行特定测试方法
pytest tests/parser/test_validator.py::TestDSLValidator::test_correct_dsl
```

### 批量测试 .kirin 文件

```bash
# 使用默认路径（从环境变量 KIRIN_TEST_ROOT 读取）
pytest tests/parser/test_parser_all_kirin.py

# 指定测试目录
pytest tests/parser/test_parser_all_kirin.py --kirin-root "E:/dataset/Navi/final_thesis_datas/ori_dsl"
```

## 测试标记

使用 pytest 标记来分类测试：

```bash
# 只运行单元测试
pytest -m unit

# 运行需要 LLM 的测试（默认会跳过）
pytest -m requires_llm

# 跳过需要 LLM 的测试（这是默认行为）
pytest -m "not requires_llm"

# 跳过需要测试数据的测试
pytest -m "not requires_data"

# 只运行 parser 包下的测试
pytest tests/parser/

# 运行所有测试（包括需要 LLM 的）
pytest -m ""  # 或者修改 pyproject.toml 中的 addopts
```

## 环境变量

- `KIRIN_TEST_ROOT`: 默认的 .kirin 文件测试根目录
- `TEST_DATA_BASE`: 测试数据的基础路径

## 依赖处理

### ANTLR 相关测试

ANTLR 是必需的依赖，测试会直接导入。如果 `antlr4-python3-runtime` 未安装或 ANTLR 解析器未生成，测试会在导入时报错。

**安装 ANTLR 依赖：**
```bash
uv sync  # 这会安装 pyproject.toml 中的依赖，包括 antlr4-python3-runtime
```

**生成 ANTLR 解析器：**
```bash
cd script/app/refine/parser
# Windows
generate_antlr_parser.bat
# Linux/macOS
./generate_antlr_parser.sh
```

### OpenAI 相关测试

需要 LLM 的测试默认会被跳过（在 `pyproject.toml` 中配置了 `-m "not requires_llm"`）。

**运行需要 LLM 的测试：**
```bash
# 运行所有需要 LLM 的测试
pytest -m requires_llm

# 运行所有测试（包括需要 LLM 的）
pytest -m ""  # 覆盖默认的标记过滤

# 或者临时修改 pyproject.toml 中的 addopts，移除 "-m", "not requires_llm"
```

## 输出

测试报告会保存在临时目录中（pytest 自动管理），可以通过 `tmp_path` fixture 访问。

对于批量测试，报告会输出到控制台并保存到临时文件。
