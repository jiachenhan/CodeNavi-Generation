import json
import os
from pathlib import Path

import utils
from app.abs.select_methods import LLM_4_ROUND, navi_abstract, LLM_GENPAT_4_ROUND
from app.communication import PatternInput
from app.abs.classified_topdown.inference import Analyzer
from interface.java.run_java_api import java_llm_abstract, java_genpat_abstract
from interface.llm.llm_openai import LLMOpenAI
from utils.common import timeout
from utils.config import LoggerConfig, set_config, PipelineConfig

_logger = LoggerConfig.get_logger(__name__)





"""
这个修饰器不支持直接在这个文件作为入口函数时使用
在spawn过程中报错: Can't Pickle <function run_llm_analysis ...>: it's not the same object as __main__.run_llm_analysis
"""



def do_abstract(use_llm: bool = False):
    if use_llm:
        _config = set_config("yunwu")
        _llm = LLMOpenAI(base_url=_config.get("openai").get("base_url"),
                        api_key=_config.get("openai").get("api_keys")[0],
                        model_name=_config.get("openai").get("model"))

        _pattern_input = PatternInput.from_file(PipelineConfig.pattern_info_path)
        navi_abstract(_llm, _pattern_input, PipelineConfig.pattern_output_path, LLM_GENPAT_4_ROUND)
        java_llm_abstract(10,
                          PipelineConfig.pattern_ori_path,
                          PipelineConfig.pattern_output_path,
                          PipelineConfig.pattern_abs_path,
                          PipelineConfig.jar_path
                          )
    else:
        java_genpat_abstract(10,
                             PipelineConfig.pattern_ori_path,
                             PipelineConfig.pattern_abs_path,
                             PipelineConfig.jar_path)


def inner_main():
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
    navi_abstract(llm, pattern_input, pattern_output_path)
    # 生成修改后pattern
    java_llm_abstract(10, pattern_ori_path, pattern_output_path, pattern_abs_path, jar_path)


if __name__ == "__main__":
    _config = set_config("ppinfra")
    jar_path = _config.get("jar_path")

    llm = LLMOpenAI(base_url=_config.get("openai").get("base_url"),
                    api_key=_config.get("openai").get("api_keys")[0],
                    model_name=_config.get("openai").get("model"))

    dataset_name = "codeql_sampled_v1"
    checker = "Random_used_only_once"
    group = "1"
    case = "2"

    dataset_path = Path("/data/jiangjiajun/CodeNavi-DSL/data") / dataset_name / checker / group / case

    case_info = json.load(open(dataset_path / "info.json", 'r'))["may_be_fixed_violations"].strip()

    pattern_ori_path = utils.config.get_pattern_base_path() / dataset_name / "ori" / checker / group / f"{case}.ser"
    pattern_abs_path = utils.config.get_pattern_base_path() / dataset_name / "abs" / checker / group / f"{case}.ser"

    pattern_info_path = utils.config.get_pattern_info_base_path() / dataset_name / "input" /  checker / group / f"{case}.json"
    pattern_output_path = utils.config.get_pattern_info_base_path() / dataset_name / "output" / checker / group / f"{case}.json"

    # 调用LLM抽象
    pattern_input = PatternInput.from_file(pattern_info_path)
    pattern_input.set_error_info(case_info)

    # llm_abstract(llm, pattern_input, pattern_output_path)
    # 生成修改后pattern
    java_llm_abstract(10, pattern_ori_path, pattern_output_path, pattern_abs_path, jar_path)
        