"""
测试解析所有 .kirin 文件

使用 pytest 框架重写的测试，用于批量测试 DSL 解析器。
每个 .kirin 文件都会作为一个独立的单元测试。
"""
import pytest
from pathlib import Path
from typing import List, Dict, Tuple, Optional, Generator
import os

# DSLParser 是必需的依赖，直接导入（如果失败会在导入时报错）
from app.refine.parser import DSLParser
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def get_kirin_root_path(config_or_request) -> Path:
    """
    获取 .kirin 文件的根目录路径
    
    Args:
        config_or_request: pytest config 对象或 request 对象
        
    Returns:
        根目录路径
    """
    root_dir = config_or_request.config.getoption("--kirin-root", default=None)
    if root_dir:
        return Path(root_dir)
    else:
        default_path = os.getenv("KIRIN_TEST_ROOT", "E:/dataset/Navi/final_thesis_datas/ori_dsl")
        return Path(default_path)


def find_all_kirin_files(root_dir: Path) -> Generator[Path, None, None]:
    """
    递归查找所有 .kirin 文件（生成器）
    
    Args:
        root_dir: 根目录路径
        
    Yields:
        .kirin 文件路径
    """
    if not root_dir.exists():
        _logger.error(f"Directory does not exist: {root_dir}")
        return
    
    for file_path in sorted(root_dir.rglob("*.kirin")):
        yield file_path


def get_relative_path(file_path: Path, base_path: Optional[Path] = None) -> str:
    """
    获取相对路径字符串
    
    Args:
        file_path: 文件路径
        base_path: 基准路径（如果为None，则使用文件路径的根目录）
        
    Returns:
        相对路径字符串，例如：pmd_v2_commits\\UseCollectionIsEmpty\\10\\2.kirin
    """
    if base_path is None:
        # 尝试找到包含常见目录结构的基准路径
        # 例如：.../ori_dsl/pmd_v2_commits/...
        parts = file_path.parts
        # 查找包含常见模式的部分（如 pmd_v2_commits, codeql_sampled_v1 等）
        for i, part in enumerate(parts):
            if any(pattern in part for pattern in ['pmd_', 'codeql_', 'sampled_', 'commits']):
                # 从这部分开始构建相对路径
                rel_parts = parts[i:]
                return os.path.join(*rel_parts)
        
        # 如果找不到，使用文件名
        return file_path.name
    else:
        try:
            rel_path = file_path.relative_to(base_path)
            # 使用 Path 对象转换为字符串，然后替换路径分隔符
            return str(rel_path).replace('/', os.sep)
        except ValueError:
            # 如果无法计算相对路径，返回文件名
            return file_path.name


def parse_kirin_file(file_path: Path) -> Tuple[bool, Optional[str], Optional[Dict], Optional[List[str]]]:
    """
    解析单个 .kirin 文件
    
    Args:
        file_path: .kirin 文件路径
        
    Returns:
        (success, error_message, parse_info, error_details)
        - success: 是否解析成功
        - error_message: 错误信息（如果失败）
        - parse_info: 解析信息字典（如果成功）
        - error_details: 详细错误信息列表（如果失败）
    """
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            dsl_code = f.read()
        
        if not dsl_code.strip():
            return False, "File is empty", None, None
        
        # 使用解析器解析
        parser = DSLParser(dsl_code)
        root_query = parser.parse()
        
        if root_query is None:
            # 收集详细错误信息
            parse_errors = parser.get_parse_errors()
            error_position = parser.get_error_position()
            
            # 构建错误消息
            error_parts = []
            if parse_errors.has_errors():
                # ANTLR 错误信息已经包含行号和列号
                error_parts.extend(parse_errors.to_string_list())
            else:
                error_parts.append("Parser returned None (parse failed)")
            
            # 添加位置信息（如果错误信息中没有包含位置信息）
            if error_position is not None:
                line_num = dsl_code[:error_position].count('\n') + 1
                last_newline = dsl_code.rfind('\n', 0, error_position)
                col_num = error_position - last_newline if last_newline >= 0 else error_position + 1
                # 检查错误信息中是否已包含位置信息
                has_position_info = any('Line' in err or 'position' in err.lower() for err in parse_errors.to_string_list())
                if not has_position_info:
                    error_parts.append(f"Error at position {error_position} (Line {line_num}, Column {col_num})")
            
            return False, "Parser returned None (parse failed)", None, error_parts
        
        # 收集解析信息
        all_queries = parser.get_all_queries()
        # node_map 是解析器的属性
        node_map = parser.node_map
        
        parse_info = {
            'entity_type': root_query.entity.node_type,
            'alias': root_query.entity.alias,
            'total_queries': len(all_queries),
            'named_nodes': len(node_map),
            'file_size': len(dsl_code),
            'line_count': dsl_code.count('\n') + 1,
        }
        
        return True, None, parse_info, None
        
    except Exception as e:
        error_msg = f"Exception during parsing: {str(e)}"
        _logger.error(f"Error parsing {file_path}: {error_msg}", exc_info=True)
        return False, error_msg, None, [error_msg, f"Exception type: {type(e).__name__}"]


def pytest_generate_tests(metafunc: pytest.Metafunc):
    """
    动态生成测试用例
    
    为每个 .kirin 文件生成一个独立的测试用例。
    每个文件都会作为一个独立的单元测试运行。
    """
    if "kirin_file" in metafunc.fixturenames:
        # 获取文件列表
        root_path = get_kirin_root_path(metafunc)
        
        # 将生成器转换为列表
        files = list(find_all_kirin_files(root_path))
        
        if not files:
            # 如果没有文件，跳过测试
            pytest.skip(f"No .kirin files found in {root_path}")
        
        # 为每个文件生成一个测试用例
        # 使用相对路径作为测试 ID
        test_ids = []
        for file_path in files:
            rel_path = get_relative_path(file_path, root_path)
            test_ids.append(rel_path)
        
        # 参数化测试，每个文件作为一个独立的测试用例
        metafunc.parametrize("kirin_file", files, ids=test_ids)


def test_parse_single_kirin_file(kirin_file: Path, request: pytest.FixtureRequest):
    """
    测试解析单个 .kirin 文件
    
    每个 .kirin 文件都会作为独立的测试用例运行。
    使用 pytest 参数化，让每个文件都成为一个单元测试。
    
    Args:
        kirin_file: .kirin 文件路径（通过 pytest_generate_tests 参数化传入）
        request: pytest request 对象（用于获取配置）
    """
    # 获取基准路径用于计算相对路径
    base_path = get_kirin_root_path(request)
    
    # 获取相对路径用于输出
    rel_path = get_relative_path(kirin_file, base_path)
    
    # 解析文件
    success, error_msg, parse_info, error_details = parse_kirin_file(kirin_file)
    
    if success:
        # 解析成功
        _logger.info(f"✓ Success: {rel_path}")
        _logger.debug(f"  Parse info: {parse_info}")
        assert parse_info is not None, "Parse info should not be None for successful parse"
        assert parse_info['total_queries'] > 0, "Should have at least one query"
    else:
        # 解析失败，构建详细的错误消息
        error_message = f"Failed to parse {rel_path}"
        if error_msg:
            error_message += f": {error_msg}"
        
        if error_details:
            error_message += "\nDetailed errors:\n"
            for detail in error_details:
                error_message += f"  {detail}\n"
        
        # 使用相对路径在断言消息中
        pytest.fail(error_message)


def pytest_addoption(parser):
    """添加 pytest 命令行选项"""
    parser.addoption(
        "--kirin-root",
        action="store",
        default=None,
        help="Root directory containing .kirin files to test"
    )
