from pathlib import Path
from typing import Generator

from app.basic_modification_analysis import background_analysis
from app.communication import InputSchema
from app.select_elements import ElementAnalysis
from interface.llm.llm_dispatcher import LLMDispatcher
from interface.llm.llm_openai import LLMOpenAI
from utils.config import get_pattern_info_base_path, set_proxy
from utils.timer import Timer


def execute_task(_llm,
                 _global_schema: InputSchema,
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
                _global_schema = InputSchema.parse_file(_json_file)
            except Exception as e:
                print(f"Error: {e}")
                continue
            _view_path = _output_path / dataset_path.stem / _group_num / f"{_file_name}.json"

            yield {
                "func": execute_task,
                "args": (_global_schema, _view_path,)
            }


if __name__ == "__main__":
    set_proxy()
    deepseek_instances1 = [
        LLMOpenAI(base_url="https://api.deepseek.com", api_key="sk-92e516aab3d443adb30c6659284163e8",
                  model_name="deepseek-chat")
        for _ in range(100)
    ]

    # deepseek_instances2 = [
    #     LLMOpenAI(base_url="https://api.deepseek.com", api_key="sk-a52d51021f214e27a8eb6d12fa18a0ff",
    #               model_name="deepseek-chat")
    #     for _ in range(50)
    # ]
    #
    # deepseek_instances3 = [
    #     LLMOpenAI(base_url="https://api.deepseek.com", api_key="sk-5dd60037665f4ae6995140fced285412",
    #               model_name="deepseek-chat")
    #     for _ in range(50)
    # ]
    #
    # deepseek_instances4 = [
    #     LLMOpenAI(base_url="https://api.deepseek.com", api_key="sk-a467d9d3a9a34c5ca4a45ee6add34e68",
    #               model_name="deepseek-chat")
    #     for _ in range(50)
    # ]

    deepseek_instances = deepseek_instances1
    dispatcher = LLMDispatcher(deepseek_instances)

    input_path = get_pattern_info_base_path() / "input" / "c3_random_1000"
    output_path = get_pattern_info_base_path() / "output" / "c3_random_1000"

    tasks = task_generator(input_path, output_path)
    dispatcher.submit_tasks(tasks)
