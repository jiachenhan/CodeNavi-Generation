import concurrent.futures
from pathlib import Path

import utils
from data.dataset import DataCollection, OneMethodFilePair
from interface.java.run_java_api import java_extract_pattern
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def extract_pattern(_collection_name: str,
                    _dataset_name: str,
                    _group: str,
                    _pairs: list[OneMethodFilePair],
                    _jar_path: str,
                    timeout_sec: float):
    _logger.info(f"Starting extract_pattern for dataset {_dataset_name}, group {_group}, "
                 f"case {_pairs[0].case_path.stem if _pairs else 'N/A'}")
    if len(_pairs) < 2:
        _logger.error(f"dataset {_dataset_name}, Group {_group} has less than 2 pairs")
        return
    pattern_pair: OneMethodFilePair = _pairs[0]

    pattern_ori_ser_path = (utils.config.get_pattern_base_path() / "ori"
                            / _collection_name / _dataset_name
                            / _group / f"{pattern_pair.case_path.stem}.ser")
    pattern_input_json_path = (utils.config.get_pattern_info_base_path() / "input" /
                               _collection_name / _dataset_name
                               / _group / f"{pattern_pair.case_path.stem}.json")

    java_extract_pattern(timeout_sec, pattern_pair.case_path,
                         pattern_ori_ser_path, pattern_input_json_path, _jar_path)


def process(_data_collection: DataCollection,
            _max_threads: int,
            _jar_path: str,
            timeout_sec: float):
    # 创建线程池，并固定线程数量
    with concurrent.futures.ThreadPoolExecutor(max_workers=_max_threads) as executor:
        futures = []
        for dataset in _data_collection.datasets:
            for group, pairs in dataset.get_datas():
                futures.append(executor.submit(extract_pattern,
                                               _data_collection.collection_name, dataset.name,
                                               group, pairs, _jar_path, timeout_sec))

        # 等待所有任务完成
        concurrent.futures.wait(futures)

    print("All tasks completed.")


if __name__ == "__main__":
    cluster_path = Path("/data/jiangjiajun/LLMPAT/temporary/c3")
    names = ["junit1"]
    data_collection = DataCollection(cluster_path, names=names, grouped=True)
    jar_path = ("/data/jiangjiajun/LLMPAT/ModifyMeta/ModifiedMetaModel/artifacts/"
                "ModifiedMetaModel-1.0-SNAPSHOT-runnable.jar")
    # extract pattern
    process(data_collection, 20, jar_path, 60.0)
