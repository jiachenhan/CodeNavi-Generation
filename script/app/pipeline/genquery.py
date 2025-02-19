import utils.config
from interface.java.run_java_api import java_generate_query
from utils.config import PipelineConfig


def do_generate_dsl(dsl_path):
    java_generate_query(60, PipelineConfig.pattern_abs_path, dsl_path, PipelineConfig.jar_path)


if __name__ == "__main__":
    jar_path = utils.config.get_jar_path()

    dataset_name = "sample_100_dataset"
    group = "0bea84025c7545adbaacef130eea46cd"

    output_query_dir_path = utils.config.get_dsl_base_path() / dataset_name / f"{group}.kirin"
    pattern_abs_path = utils.config.get_pattern_base_path() / dataset_name / "abs" / f"{group}.ser"

    java_generate_query(60, pattern_abs_path, output_query_dir_path, jar_path)

