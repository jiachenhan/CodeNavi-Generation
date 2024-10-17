from pathlib import Path
from typing import Generator

from app.basic_modification_analysis import background_analysis
from app.communication import InputSchema
from app.select_elements import ElementAnalysis
from interface.llm.llm_dispatcher import LLMDispatcher
from interface.llm.llm_openai import LLMOpenAI
from utils.config import get_pattern_info_base_path
from utils.timer import Timer


def execute_task(_llm,
                 _global_schema: InputSchema,
                 _view_path: Path) -> None:
    with Timer():
        # 调用背景分析
        _background_history = background_analysis(_llm, _global_schema)
        # 进行元素分析
        _element_analysis = ElementAnalysis(_llm, _global_schema)
        _element_analysis.analysis(_background_history)

        _element_analysis.view(_view_path)


def task_generator(_input_path: Path, _output_path: Path) -> Generator:
    for _json_file in _input_path.rglob("*.json"):
        _group_num = _json_file.parent.name
        _file_name = _json_file.stem

        _global_schema = InputSchema.parse_file(_json_file)
        _view_path = _output_path / _group_num / f"{_file_name}.json"

        yield {
            "func": execute_task,
            "args": (_global_schema, _view_path, )
        }


if __name__ == "__main__":
    codeLlama0 = LLMOpenAI(base_url="http://localhost:8001/v1", api_key="empty", model_name="CodeLlama")
    codeLlama2 = LLMOpenAI(base_url="http://localhost:8002/v1", api_key="empty", model_name="CodeLlama")
    codeLlama4 = LLMOpenAI(base_url="http://localhost:8003/v1", api_key="empty", model_name="CodeLlama")
    codeLlama6 = LLMOpenAI(base_url="http://localhost:8004/v1", api_key="empty", model_name="CodeLlama")

    dispatcher = LLMDispatcher([codeLlama0, codeLlama2, codeLlama4, codeLlama6])

    input_path = get_pattern_info_base_path() / "input" / "c3" / "drjava1"
    output_path = get_pattern_info_base_path() / "output" / "c3" / "drjava1"

    tasks = task_generator(input_path, output_path)
    dispatcher.submit_tasks(tasks)
