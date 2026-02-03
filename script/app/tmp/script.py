from pathlib import Path

from utils.config import get_root_project_path, LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def delete_kirin_log():
    directory = get_root_project_path()

    for log_file in directory.glob("kirin*.log"):
        _logger.info(f"delete file: {log_file}")
        log_file.unlink()


def delete_navie_refine_kirin_files():
    directory = Path("E:/dataset/Navi/final_thesis_datas/ori_dsl")

    for naive_refine_file in directory.rglob("*_refine.kirin"):
        _logger.info(f"delete file: {naive_refine_file}")
        naive_refine_file.unlink()


if __name__ == "__main__":
    # delete_kirin_log()
    delete_navie_refine_kirin_files()
