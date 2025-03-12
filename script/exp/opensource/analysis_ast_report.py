from pathlib import Path
from typing import Generator

from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def split_reports(report_path: Path) -> Generator[str, None, None]:
    current_section = []
    with open(report_path, "r", encoding="utf-8") as file:
        for line in file:
            if line.strip().startswith('hash#'):
                if current_section:  # 遇到新分隔符时保存当前部分
                    yield '\n'.join(current_section).strip()
                else:
                    current_section = [line.strip()]
            else:
                current_section.append(line.strip())

        if current_section:  # 处理最后一部分
            yield '\n'.join(current_section).strip()

def collect_report(report_path: Path) -> list:
    result = []
    for _method_section in split_reports(report_path):
        _method_info = _method_section.split("\n")[0]
        if not _method_info.startswith("hash#"):
            _logger.error(f"Invalid method info: {_method_info}")
            continue

        _method_info_relative_path = _method_info.split("#")[1]
        _method_signature = _method_info.split("#")[-1]
        result.append({"path": _method_info_relative_path, "signature": _method_signature})
    return result