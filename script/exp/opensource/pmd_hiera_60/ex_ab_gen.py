import os
from pathlib import Path
from typing import Generator

import utils
from app.communication import PatternInput
from app.pipeline.abstract import navi_abstract
from interface.java.run_java_api import java_extract_pattern, java_llm_abstract, java_generate_query
from interface.llm.llm_api import LLMAPI
from interface.llm.llm_openai import LLMOpenAI
from utils.config import set_config, LoggerConfig

_logger = LoggerConfig.get_logger(__name__)

def extract_pattern(_jar: str, _case_path: Path, _pattern_path: Path, _pattern_info_path: Path):
    group_name = _case_path.parent.stem
    pattern_ori_path = _pattern_path / "ori" / group_name / f"{_case_path.stem}.ser"
    pattern_info_input_path = _pattern_info_path / "input" / group_name / f"{_case_path.stem}.json"
    java_extract_pattern(10, _case_path, pattern_ori_path, pattern_info_input_path, _jar)


def abstract_pattern(_llm: LLMAPI, _jar: str, _case_path: Path, _pattern_path: Path, _pattern_info_path: Path):
    group_name = _case_path.parent.stem
    pattern_ori_path = _pattern_path / "ori" / group_name / f"{_case_path.stem}.ser"
    pattern_abs_path = _pattern_path / "abs" / group_name / f"{_case_path.stem}.ser"
    pattern_info_input_path = _pattern_info_path / "input" / group_name / f"{_case_path.stem}.json"
    pattern_info_output_path = _pattern_info_path / "output" / group_name / f"{_case_path.stem}.json"

    if pattern_abs_path.exists():
        return

    pattern_input = PatternInput.from_file(pattern_info_input_path)
    navi_abstract(_llm, pattern_input, pattern_info_output_path)
    java_llm_abstract(10, pattern_ori_path, pattern_info_output_path, pattern_abs_path, _jar)


def generate_query(_jar: str, _case_path: Path, _pattern_path: Path, _dsl_path: Path):
    group_name = _case_path.parent.stem
    pattern_abs_path = _pattern_path / "abs" / group_name / f"{_case_path.stem}.ser"
    dsl_output_path = _dsl_path / group_name / f"{_case_path.stem}.kirin"

    java_generate_query(10, pattern_abs_path, dsl_output_path, _jar)


def get_code_pair(_path: Path, _case_name) -> Generator[Path, None, None]:
    for group in _path.iterdir():
        if not group.is_dir():
            continue
        case_path = group / _case_name
        yield case_path


if __name__ == "__main__":
    set_config("ppinfra")
    llm = LLMOpenAI(base_url=os.environ.get("OPENAI_BASE_URL"),
                    api_key=os.environ.get("OPENAI_API_KEY"),
                    model_name=os.environ.get("MODEL_NAME"))
    jar_path = utils.config.get_jar_path()

    dataset_name = "pmd_hiera_60"
    dataset_path = Path("/data/jiangjiajun/CodeNavi-DSL/data") / dataset_name

    pattern_path = utils.config.get_pattern_base_path() / dataset_name
    pattern_info_path = utils.config.get_pattern_info_base_path() / dataset_name
    dsl_path = utils.config.get_dsl_base_path() / dataset_name

    cases = get_code_pair(dataset_path, "0")

    for case in cases:
        _logger.info(f"group: {case.parent}")
        extract_pattern(jar_path, case, pattern_path, pattern_info_path)
        abstract_pattern(llm, jar_path, case, pattern_path, pattern_info_path)
        generate_query(jar_path, case, pattern_path, dsl_path)
    pass