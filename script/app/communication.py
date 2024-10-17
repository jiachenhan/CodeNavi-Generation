from typing import Union

from pydantic import BaseModel, Field

from utils.config import get_pattern_info_base_path


class InputSchema(BaseModel):
    path: str = Field(alias="FileName")
    before_code: list[str] = Field(alias="BeforeCode")
    diff: list[dict[str, Union[str, int, list[str]]]] = Field(alias="Diff")
    tree: dict = Field(alias="Before0Tree")
    attrs: dict = Field(alias="Attrs")


def pretty_print_history(history: list):
    for message in history:
        print(f"{message['role']}: {message['content']}")


if __name__ == "__main__":
    file_path = get_pattern_info_base_path() / "pattern.json"
    global_schema = InputSchema.parse_file(file_path)
    print(global_schema.path)
    print(global_schema.before_code)