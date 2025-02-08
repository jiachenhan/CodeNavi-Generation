from pathlib import Path

import utils.config
from interface.java.run_java_api import java_extract_pattern

if __name__ == '__main__':
    jar_path = utils.config.get_jar_path()

    dataset_path = Path("D:/datas/sample_100_dataset")
    dataset_name = "sample_100_dataset"
    group = "0bea84025c7545adbaacef130eea46cd"
    pattern_ori_path = utils.config.get_pattern_base_path() / dataset_name / "ori" / f"{group}.ser"
    pattern_info_path = utils.config.get_pattern_info_base_path() / dataset_name / "input" / f"{group}.json"
    java_extract_pattern(5, dataset_path, pattern_ori_path, pattern_info_path, jar_path)
