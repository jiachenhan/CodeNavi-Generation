import os
from pathlib import Path

import utils
from app.communication import PatternInput
from app.abs.classified_topdown.inference import Analyzer
from interface.java.run_java_api import java_abstract
from interface.llm.llm_openai import LLMOpenAI
from utils.config import LoggerConfig, set_config

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

    dataset_path = Path("D:/datas/sample_100_dataset")
    dataset_name = "sample_100_dataset"
    group = "0bea84025c7545adbaacef130eea46cd"
    pattern_ori_path = utils.config.get_pattern_base_path() / dataset_name / "ori" / f"{group}.ser"
    pattern_abs_path = utils.config.get_pattern_base_path() / dataset_name / "abs" / f"{group}.ser"

    pattern_info_path = utils.config.get_pattern_info_base_path() / dataset_name / "input" / f"{group}.json"
    pattern_output_path = utils.config.get_pattern_info_base_path() / dataset_name / "output" / f"{group}.json"

    # 调用LLM抽象
    pattern_input = PatternInput.from_file(pattern_info_path)
    llm_abstract(llm, pattern_input, pattern_output_path)
    # 生成修改后pattern
    java_abstract(10, pattern_ori_path, pattern_output_path, pattern_abs_path, jar_path)
        