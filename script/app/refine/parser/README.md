# DSL解析器

本目录包含DSL语法解析器的实现，使用ANTLR4进行语法解析。

## 文件结构

- `dsl_ast.py` - DSL AST数据结构定义（与解析实现无关）
- `dsl_parser_antlr.py` - 基于ANTLR的解析器实现
- `DSL.g4` - ANTLR语法定义文件
- `generate_antlr_parser.bat` - Windows批处理脚本（生成解析器）
- `generate_antlr_parser.sh` - Linux/macOS Shell脚本（生成解析器）

## 快速开始

### 1. 安装依赖

```bash
pip install antlr4-python3-runtime>=4.13.0
```

### 2. 安装ANTLR工具

#### Windows:
```bash
# 使用Chocolatey
choco install antlr4

# 或下载JAR文件
# 从 https://www.antlr.org/download.html 下载 antlr-4.13.0-complete.jar
```

#### macOS:
```bash
brew install antlr
```

#### Linux:
```bash
# 下载ANTLR4 JAR文件
wget https://www.antlr.org/download/antlr-4.13.0-complete.jar
```

### 3. 生成解析器

#### Windows:
```bash
cd app/refine/parser
generate_antlr_parser.bat
```

#### Linux/macOS:
```bash
cd app/refine/parser
chmod +x generate_antlr_parser.sh
./generate_antlr_parser.sh
```

#### 手动生成:
```bash
cd app/refine/parser
mkdir -p antlr_generated
antlr4 -Dlanguage=Python3 -o antlr_generated -visitor -listener DSL.g4
```

### 4. 使用解析器

```python
from app.refine.parser import DSLParser, Query

dsl_code = """
functionCall fc where fc.name == "test" ;
"""

parser = DSLParser(dsl_code)
root_query = parser.parse()

if root_query:
    print(f"Entity: {root_query.entity.node_type}")
    print(f"Alias: {root_query.entity.alias}")
else:
    print("Parse failed!")
    for error in parser.get_parse_errors():
        print(f"Error: {error}")
```

## 语法文件说明

`DSL.g4` 定义了DSL的完整语法规则，包括：

- **主查询**: `EntityDecl where Condition ;`
- **嵌套查询**: `EntityDecl [where Condition] [;|,|)]`（用于contain/in中）
  - where子句是可选的
  - 结束符可以是分号、逗号或右括号（取决于上下文）

## 注意事项

1. **生成的文件位置**: ANTLR生成的文件应该在 `antlr_generated/` 目录下
2. **版本兼容性**: 建议使用ANTLR 4.13.0或更高版本
3. **导入警告**: 在生成解析器之前，IDE可能会显示导入警告，这是正常的

## 故障排除

### 问题1: ImportError: No module named 'DSLLexer'

**原因**: 未生成ANTLR解析器代码

**解决**: 运行生成脚本或手动执行 `antlr4` 命令

### 问题2: antlr4 command not found

**原因**: ANTLR工具未安装或不在PATH中

**解决**: 
- 安装ANTLR工具（见步骤2）
- 或使用JAR文件: `java -jar antlr-4.13.0-complete.jar -Dlanguage=Python3 -o antlr_generated DSL.g4`

### 问题3: 语法错误

**原因**: DSL.g4语法文件有错误

**解决**: 检查语法文件，使用ANTLR工具验证: `antlr4 DSL.g4`（会显示语法错误）
