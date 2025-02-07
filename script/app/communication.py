from pathlib import Path
from typing import Union

from pydantic import BaseModel, Field

from utils.config import get_pattern_info_base_path, LoggerConfig

_logger = LoggerConfig.get_logger(__name__)

class PatternInput(BaseModel):
    path: str = Field(alias="FileName")
    before_code: list[str] = Field(alias="BeforeCode")
    diff: list[dict[str, Union[str, int, list[str]]]] = Field(alias="Diff")
    tree: dict = Field(alias="Before0Tree")
    attrs: dict = Field(alias="Attrs")

    @classmethod
    def from_file(cls, _file_path: Path) -> "PatternInput":
        try:
            file_content = _file_path.read_text(encoding="utf-8")
            return cls.model_validate_json(file_content)
        except FileNotFoundError:
            _logger.error(f"File {_file_path} not found.")
            raise
        except Exception as e:
            _logger.error(f"Error parsing file {_file_path}: {e}")
            raise


def pretty_print_history(history: list):
    for message in history:
        print(f"{message['role']}: {message['content']}")


if __name__ == "__main__":
    file_path = get_pattern_info_base_path() / "pattern.json"
    global_schema = PatternInput.from_file(file_path)
    print(global_schema.path)
    print(global_schema.before_code)