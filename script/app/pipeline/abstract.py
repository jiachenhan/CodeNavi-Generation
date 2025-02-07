import os
from pathlib import Path

import utils
from app.communication import PatternInput
from app.inference import Analyzer
from interface.java.run_java_api import java_abstract
from interface.llm.llm_openai import LLMOpenAI
from utils.config import LoggerConfig, set_config, get_pattern_base_path, get_pattern_info_base_path

_logger = LoggerConfig.get_logger(__name__)


def llm_abstract(_llm,
                 _pattern_input: PatternInput,
                 _output_path: Path) -> None:
    try:
        analyzer = Analyzer(_llm, _pattern_input)
        analyzer.analysis()
        analyzer.serialize(_output_path)
    except Exception as e:
        _logger.error(f"Error in {_output_path}: {e}")
        return

if __name__ == "__main__":
    set_config()
    llm = LLMOpenAI(base_url=os.environ.get("OPENAI_BASE_URL"),
                    api_key=os.environ.get("OPENAI_API_KEY"),
                    model_name=os.environ.get("MODEL_NAME"))

    jar_path = utils.config.get_jar_path()
    dataset_name = "sample_100_dataset"

    group_names = []

    for group_name in group_names:
        pattern_ori_ser_path = get_pattern_base_path() / "ori" / dataset_name / f"{group_name}.ser"
        pattern_abs_ser_path = get_pattern_base_path() / "abs" / dataset_name / f"{group_name}.ser"

        if pattern_abs_ser_path.exists():
            continue

        input_path = get_pattern_info_base_path() / "input" / dataset_name / f"{group_name}.json"
        output_path = get_pattern_info_base_path() / "output" / dataset_name / f"{group_name}.json"

        pattern_input = PatternInput.from_file(input_path)

        llm_abstract(llm, pattern_input, output_path)
        java_abstract(10, pattern_ori_ser_path, pattern_abs_ser_path, output_path, jar_path)
        