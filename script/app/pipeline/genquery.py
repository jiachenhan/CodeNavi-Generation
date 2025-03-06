from pathlib import Path

import utils.config
from interface.java.run_java_api import java_generate_query
from utils.config import PipelineConfig


def do_generate_dsl(dsl_path):
    java_generate_query(60, PipelineConfig.pattern_abs_path, dsl_path, PipelineConfig.jar_path)


def inner_main():
    jar_path = utils.config.get_jar_path()

    dataset_name = "sample_100_dataset"
    group = "0bea84025c7545adbaacef130eea46cd"

    output_query_dir_path = utils.config.get_dsl_base_path() / dataset_name / f"{group}.kirin"
    pattern_abs_path = utils.config.get_pattern_base_path() / dataset_name / "abs" / f"{group}.ser"

    java_generate_query(60, pattern_abs_path, output_query_dir_path, jar_path)


if __name__ == "__main__":
    _config = utils.config.set_config("ppinfra")
    jar_path = _config.get("jar_path")

    dataset_name = "codeql_sampled_v1"
    checker = "Random_used_only_once"
    group = "1"
    case = "2"

    output_query_dir_path = utils.config.get_dsl_base_path() / dataset_name / checker / group / f"{case}.kirin"
    pattern_abs_path = utils.config.get_pattern_base_path() / dataset_name / "abs" / checker / group / f"{case}.ser"

    java_generate_query(60, pattern_abs_path, output_query_dir_path, jar_path)
