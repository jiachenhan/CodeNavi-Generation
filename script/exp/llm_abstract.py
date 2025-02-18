import os
from pathlib import Path
from typing import Generator

from app.basic_modification_analysis import background_analysis
from app.communication import PatternInput
from app.select_elements import ElementAnalysis
from interface.llm.llm_dispatcher import LLMDispatcher
from interface.llm.llm_openai import LLMOpenAI
from utils.config import get_pattern_info_base_path, set_config
from utils.common import Timer


def execute_task(_llm,
                 _global_schema: PatternInput,
                 _view_path: Path) -> None:
    with Timer():
        print(f"Processing {_view_path}")
        try:
            # 调用背景分析
            _background_history = background_analysis(_llm, _global_schema)
            # 进行元素分析
            _element_analysis = ElementAnalysis(_llm, _global_schema)
            _element_analysis.analysis(_background_history)

            _element_analysis.view(_view_path)
        except Exception as e:
            print(f"Error in {_view_path}: {e}")
            return


def task_generator(_input_path: Path, _output_path: Path) -> Generator:
    for dataset_path in _input_path.iterdir():
        for _json_file in dataset_path.rglob("*.json"):
            _group_num = _json_file.parent.name
            _file_name = _json_file.stem

            try:
                _global_schema = PatternInput.parse_file(_json_file)
            except Exception as e:
                print(f"Error: {e}")
                continue
            _view_path = _output_path / dataset_path.stem / _group_num / f"{_file_name}.json"

            yield {
                "func": execute_task,
                "args": (_global_schema, _view_path,)
            }


if __name__ == "__main__":
    set_config("deepseek")
    deepseek_instances1 = [
        LLMOpenAI(base_url=os.environ.get("OPENAI_BASE_URL"), api_key=os.environ.get("OPENAI_API_KEY"),
                  model_name=os.environ.get("MODEL_NAME"))
    ]

    deepseek_instances = deepseek_instances1
    dispatcher = LLMDispatcher(deepseek_instances)

    input_path = get_pattern_info_base_path() / "input" / "c3_random_1000"
    output_path = get_pattern_info_base_path() / "output" / "c3_random_1000"

    tasks = task_generator(input_path, output_path)
    dispatcher.submit_tasks(tasks)
