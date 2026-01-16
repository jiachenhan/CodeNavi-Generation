#!/bin/bash
# 生成ANTLR解析器的Shell脚本（Linux/macOS）
# 需要先安装ANTLR4工具

echo "Generating ANTLR parser for DSL..."

# 检查ANTLR是否可用
if ! command -v antlr4 &> /dev/null; then
    echo "ERROR: antlr4 command not found!"
    echo "Please install ANTLR4:"
    echo "  macOS: brew install antlr"
    echo "  Linux: Download from https://www.antlr.org/download.html"
    echo "  Or use: java -jar antlr-4.13.0-complete.jar"
    exit 1
fi

# 获取脚本所在目录
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPT_DIR"

# 创建输出目录
mkdir -p antlr_generated

# 生成解析器
antlr4 -Dlanguage=Python3 -o antlr_generated -visitor -listener DSL.g4

if [ $? -eq 0 ]; then
    echo ""
    echo "SUCCESS: ANTLR parser generated successfully!"
    echo "Generated files are in: antlr_generated/"
else
    echo ""
    echo "ERROR: Failed to generate ANTLR parser"
    exit 1
fi
