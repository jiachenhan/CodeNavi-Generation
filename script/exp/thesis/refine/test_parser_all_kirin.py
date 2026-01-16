"""
测试解析所有.kirin文件

遍历指定目录下的所有.kirin文件，使用解析器进行解析测试。
统计解析成功和失败的文件，并输出详细报告。
"""
import sys
from pathlib import Path
from typing import List, Dict, Tuple, Optional
from collections import defaultdict

# 添加项目根目录到路径
project_root = Path(__file__).parent.parent.parent.parent
sys.path.insert(0, str(project_root))

from app.refine.parser import DSLParser
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def find_all_kirin_files(root_dir: Path) -> List[Path]:
    """
    递归查找所有.kirin文件
    
    Args:
        root_dir: 根目录路径
        
    Returns:
        .kirin文件路径列表
    """
    kirin_files = []
    if not root_dir.exists():
        _logger.error(f"Directory does not exist: {root_dir}")
        return kirin_files
    
    for file_path in root_dir.rglob("*.kirin"):
        kirin_files.append(file_path)
    
    return sorted(kirin_files)


def parse_kirin_file(file_path: Path) -> Tuple[bool, Optional[str], Optional[Dict], Optional[List[str]]]:
    """
    解析单个.kirin文件
    
    Args:
        file_path: .kirin文件路径
        
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
            if parse_errors:
                # ANTLR 错误信息已经包含行号和列号
                error_parts.extend(parse_errors)
            else:
                error_parts.append("Parser returned None (parse failed)")
            
            # 添加位置信息（如果错误信息中没有包含位置信息）
            if error_position is not None:
                line_num = dsl_code[:error_position].count('\n') + 1
                last_newline = dsl_code.rfind('\n', 0, error_position)
                col_num = error_position - last_newline if last_newline >= 0 else error_position + 1
                # 检查错误信息中是否已包含位置信息
                has_position_info = any('Line' in err or 'position' in err.lower() for err in parse_errors)
                if not has_position_info:
                    error_parts.append(f"Error at position {error_position} (Line {line_num}, Column {col_num})")
            
            # 添加代码预览（前10行）
            lines = dsl_code.split('\n')
            preview_lines = lines[:min(10, len(lines))]
            if len(lines) > 10:
                preview_lines.append(f"... ({len(lines) - 10} more lines)")
            
            error_parts.append("Code preview (first 10 lines):")
            for i, line in enumerate(preview_lines, 1):
                error_parts.append(f"  {i:3d}: {line}")
            
            return False, "Parser returned None (parse failed)", None, error_parts
        
        # 收集解析信息
        all_queries = parser.get_all_queries()
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


def test_all_kirin_files(root_dir: str, output_file: Optional[Path] = None):
    """
    测试解析所有.kirin文件
    
    Args:
        root_dir: 根目录路径（字符串）
        output_file: 输出报告文件路径（可选）
    """
    root_path = Path(root_dir)
    
    _logger.info(f"Searching for .kirin files in: {root_path}")
    
    # 查找所有.kirin文件
    kirin_files = find_all_kirin_files(root_path)
    
    if not kirin_files:
        _logger.warning(f"No .kirin files found in {root_path}")
        return
    
    _logger.info(f"Found {len(kirin_files)} .kirin files")
    
    # 统计信息
    success_count = 0
    fail_count = 0
    failed_files: List[Tuple[Path, str, Optional[List[str]]]] = []
    success_files: List[Tuple[Path, Dict]] = []
    
    # 按目录分组统计
    dir_stats = defaultdict(lambda: {'success': 0, 'fail': 0})
    
    # 解析每个文件
    for i, file_path in enumerate(kirin_files, 1):
        _logger.info(f"[{i}/{len(kirin_files)}] Parsing: {file_path}")
        
        success, error_msg, parse_info, error_details = parse_kirin_file(file_path)
        
        # 获取相对路径用于分组
        try:
            rel_path = file_path.relative_to(root_path)
            dir_key = str(rel_path.parent)
        except ValueError:
            dir_key = str(file_path.parent)
        
        if success:
            success_count += 1
            success_files.append((file_path, parse_info))
            dir_stats[dir_key]['success'] += 1
            _logger.debug(f"  ✓ Success: {parse_info}")
        else:
            fail_count += 1
            failed_files.append((file_path, error_msg, error_details))
            dir_stats[dir_key]['fail'] += 1
            _logger.warning(f"  ✗ Failed: {error_msg}")
    
    # 生成报告
    report_lines = []
    report_lines.append("=" * 80)
    report_lines.append("DSL Parser Test Report")
    report_lines.append("=" * 80)
    report_lines.append(f"Root Directory: {root_path}")
    report_lines.append(f"Total Files: {len(kirin_files)}")
    report_lines.append(f"Success: {success_count} ({success_count/len(kirin_files)*100:.1f}%)")
    report_lines.append(f"Failed: {fail_count} ({fail_count/len(kirin_files)*100:.1f}%)")
    report_lines.append("")
    
    # 按目录统计
    if dir_stats:
        report_lines.append("Statistics by Directory:")
        report_lines.append("-" * 80)
        for dir_key in sorted(dir_stats.keys()):
            stats = dir_stats[dir_key]
            total = stats['success'] + stats['fail']
            success_rate = stats['success'] / total * 100 if total > 0 else 0
            report_lines.append(f"  {dir_key}:")
            report_lines.append(f"    Total: {total}, Success: {stats['success']}, Failed: {stats['fail']} ({success_rate:.1f}%)")
        report_lines.append("")
    
    # 失败文件详情（包含详细错误信息）
    if failed_files:
        report_lines.append("Failed Files (with detailed error information):")
        report_lines.append("=" * 80)
        for file_path, error_msg, error_details in failed_files:
            try:
                rel_path = file_path.relative_to(root_path)
            except ValueError:
                rel_path = file_path
            report_lines.append("")
            report_lines.append(f"File: {rel_path}")
            report_lines.append("-" * 80)
            report_lines.append(f"Summary: {error_msg}")
            report_lines.append("")
            
            if error_details:
                report_lines.append("Detailed Error Information:")
                for detail in error_details:
                    report_lines.append(f"  {detail}")
            else:
                report_lines.append("  (No detailed error information available)")
            
            report_lines.append("")
    
    # 成功文件统计信息
    if success_files:
        report_lines.append("Success Files Statistics:")
        report_lines.append("-" * 80)
        
        # 统计查询数量分布
        query_counts = [info['total_queries'] for _, info in success_files]
        if query_counts:
            report_lines.append(f"  Total queries per file:")
            report_lines.append(f"    Min: {min(query_counts)}, Max: {max(query_counts)}, Avg: {sum(query_counts)/len(query_counts):.1f}")
        
        # 统计文件大小分布
        file_sizes = [info['file_size'] for _, info in success_files]
        if file_sizes:
            report_lines.append(f"  File sizes (bytes):")
            report_lines.append(f"    Min: {min(file_sizes)}, Max: {max(file_sizes)}, Avg: {sum(file_sizes)/len(file_sizes):.0f}")
        
        # 统计行数分布
        line_counts = [info['line_count'] for _, info in success_files]
        if line_counts:
            report_lines.append(f"  Line counts:")
            report_lines.append(f"    Min: {min(line_counts)}, Max: {max(line_counts)}, Avg: {sum(line_counts)/len(line_counts):.1f}")
        
        report_lines.append("")
    
    # 输出报告
    report_text = "\n".join(report_lines)
    print(report_text)
    
    # 保存到文件
    if output_file:
        output_file.parent.mkdir(parents=True, exist_ok=True)
        with open(output_file, 'w', encoding='utf-8') as f:
            f.write(report_text)
        _logger.info(f"Report saved to: {output_file}")
    
    # 返回统计信息
    return {
        'total': len(kirin_files),
        'success': success_count,
        'failed': fail_count,
        'success_rate': success_count / len(kirin_files) * 100 if kirin_files else 0,
        'failed_files': failed_files,
        'dir_stats': dict(dir_stats),
    }


if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description="Test parsing all .kirin files in a directory")
    parser.add_argument(
        "root_dir",
        type=str,
        default="E:/dataset/Navi/final_thesis_datas/ori_dsl",
        nargs="?",
        help="Root directory containing .kirin files (default: E:/dataset/Navi/final_thesis_datas/ori_dsl)"
    )
    parser.add_argument(
        "-o", "--output",
        type=str,
        help="Output report file path (optional)"
    )
    
    args = parser.parse_args()
    
    output_path = Path(args.output) if args.output else None
    if not output_path:
        # 默认输出到当前目录
        output_path = Path(__file__).parent / "parser_test_report.txt"
    
    try:
        stats = test_all_kirin_files(args.root_dir, output_path)
        
        if stats:
            print(f"\n{'='*80}")
            print(f"Summary: {stats['success']}/{stats['total']} files parsed successfully ({stats['success_rate']:.1f}%)")
            
            if stats['failed'] > 0:
                print(f"\nFailed files: {stats['failed']}")
                sys.exit(1)
            else:
                print("\nAll files parsed successfully!")
                sys.exit(0)
        else:
            sys.exit(1)
            
    except KeyboardInterrupt:
        _logger.info("Interrupted by user")
        sys.exit(130)
    except Exception as e:
        _logger.error(f"Unexpected error: {e}", exc_info=True)
        sys.exit(1)
