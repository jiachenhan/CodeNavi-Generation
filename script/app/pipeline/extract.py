from pathlib import Path

import utils.config
from interface.java.run_java_api import java_extract_pattern
from utils.config import PipelineConfig


def do_extract(dataset_path: Path):
    java_extract_pattern(5,
                         dataset_path,
                         PipelineConfig.pattern_ori_path,
                         PipelineConfig.pattern_info_path,
                         PipelineConfig.jar_path
                         )


if __name__ == '__main__':
    jar_path = utils.config.get_jar_path()

    dataset_name = "sample_100_dataset"
    group = "0bea84025c7545adbaacef130eea46cd"

    dataset_path = Path("D:/datas/sample_100_dataset") / group
    pattern_ori_path = utils.config.get_pattern_base_path() / dataset_name / "ori" / f"{group}.ser"
    pattern_info_path = utils.config.get_pattern_info_base_path() / dataset_name / "input" / f"{group}.json"
    java_extract_pattern(5, dataset_path, pattern_ori_path, pattern_info_path, jar_path)
