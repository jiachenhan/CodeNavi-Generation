import random
import concurrent.futures
from pathlib import Path
from typing import List

import utils
from data.dataset import DataCollection, OneMethodFilePair
from interface.java.run_java_api import java_genpat_detect
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def detect_bugs(_collection_name: str,
                _dataset_name: str,
                _repo_path: Path,
                _group: str,
                _pairs: list[OneMethodFilePair],
                _jar_path: str,
                timeout_sec: float):
    if len(_pairs) < 2:
        _logger.error(f"dataset {_dataset_name}, Group {_group} has less than 2 pairs")
        return
    pattern_pair: OneMethodFilePair = _pairs[0]
    buggy_pairs: List[OneMethodFilePair] = _pairs[1:]
    random.seed(utils.config.get_random_seed())
    buggy_pair: OneMethodFilePair = random.choice(buggy_pairs)

    pattern_pair_path = pattern_pair.case_path
    pattern_info_path = pattern_pair.case_path / "info.json"
    buggy_info_path = buggy_pair.case_path / "info.json"
    repo_path = _repo_path / _dataset_name
    output_path = (utils.config.get_patches_base_path() / "detect" / _collection_name
                   / _dataset_name / _group / f"{pattern_pair_path.stem}-{buggy_pair.case_path.stem}.json")
    java_genpat_detect(timeout_sec,
                       pattern_pair_path, pattern_info_path, repo_path, buggy_info_path, output_path, _jar_path)
    pass


def process(_data_collection: DataCollection,
            _repos_path: Path,
            _max_threads: int,
            _jar_path: str,
            timeout_sec: float):
    with concurrent.futures.ThreadPoolExecutor(max_workers=_max_threads) as executor:
        futures = []
        for dataset in _data_collection.datasets:
            for group, pairs in dataset.get_datas():
                futures.append(executor.submit(detect_bugs,
                                               _data_collection.collection_name, dataset.name, _repos_path,
                                               group, pairs, _jar_path, timeout_sec))

        # 等待所有任务完成
        concurrent.futures.wait(futures)

    print("All tasks completed.")


if __name__ == "__main__":
    cluster_path = Path("/data/jiangjiajun/LLMPAT/temporary/processed_data")
    repos_path = Path("/data/jiangjiajun/LLMPAT/temporary/repos")
    data_collection = DataCollection(cluster_path, names=None, grouped=True)
    jar_path = ("/data/jiangjiajun/LLMPAT/ModifyMeta/ModifiedMetaModel/artifacts/"
                "ModifiedMetaModel-1.0-SNAPSHOT-runnable.jar")
    # extract pattern
    process(data_collection, repos_path, 20, jar_path, 180.0)