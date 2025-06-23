from utils.config import get_root_project_path, LoggerConfig

_logger = LoggerConfig.get_logger(__name__)

def delete_kirin_log():
    directory = get_root_project_path()

    for log_file in directory.glob("kirin*.log"):
        _logger.info(f"delete file: {log_file}")
        log_file.unlink()


if __name__ == "__main__":
    delete_kirin_log()
