from pathlib import Path

import utils
from data.dataset import DataCollection, OneMethodFilePair
from interface.java.run_java_api import java_extract_pattern
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def extract_pattern(_data_collection: DataCollection, _jar_path: str, timeout_sec: float):
    for dataset in _data_collection.datasets:
        for group, pairs in dataset.get_datas():
            if len(pairs) < 2:
                _logger.error(f"dataset {dataset.name}, Group {group} has less than 2 pairs")
                continue

            pattern_pair: OneMethodFilePair = pairs[0]

            pattern_ori_ser_path = (utils.config.get_pattern_base_path() / "ori"
                                    / _data_collection.collection_name / dataset.name
                                    / group / f"{pattern_pair.case_path.stem}.ser")
            pattern_input_json_path = (utils.config.get_pattern_info_base_path() / "input" /
                                       _data_collection.collection_name / dataset.name
                                       / group / f"{pattern_pair.case_path.stem}.json")

            java_extract_pattern(timeout_sec, pattern_pair.case_path,
                                 pattern_ori_ser_path, pattern_input_json_path, _jar_path)


if __name__ == "__main__":
    cluster_path = Path("/data/jiangjiajun/LLMPAT/temporary/c3")
    names = ["drjava1"]
    data_collection = DataCollection(cluster_path, names=names, grouped=True)
    jar_path = ("/data/jiangjiajun/LLMPAT/ModifyMeta/ModifiedMetaModel/artifacts/"
                "ModifiedMetaModel-1.0-SNAPSHOT-runnable.jar")
    # extract pattern
    extract_pattern(data_collection, jar_path, 60.0)
