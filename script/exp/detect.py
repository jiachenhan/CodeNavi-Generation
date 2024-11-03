import random
import concurrent.futures
from pathlib import Path
from typing import List

import utils
from data.dataset import DataCollection, OneMethodFilePair
from interface.java.run_java_api import java_genpat_detect, java_detect
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def detect_bugs(_collection_name: str,
                _dataset_name: str,
                _repo_path: Path,
                _group: str,
                _pattern_path: Path,
                _buggy_pair: OneMethodFilePair,
                _jar_path: str,
                timeout_sec: float):

    buggy_info_path = _buggy_pair.case_path / "info.json"
    repo_path = _repo_path / _dataset_name
    output_path = (utils.config.get_patches_base_path() / "detect" / _collection_name / "abs_pat"
                   / _dataset_name / _group / f"{_pattern_path.stem}-{_buggy_pair.case_path.stem}.json")
    java_detect(timeout_sec,
                _pattern_path, repo_path, buggy_info_path, output_path, _jar_path)


def process(_data_collection: DataCollection,
            _repos_path: Path,
            _max_threads: int,
            _jar_path: str,
            timeout_sec: float):
    with concurrent.futures.ThreadPoolExecutor(max_workers=_max_threads) as executor:
        futures = []
        for dataset in _data_collection.datasets:
            for group, pairs in dataset.get_datas():
                pattern_group_path = (utils.config.get_pattern_base_path() / "abs" / _data_collection.collection_name /
                                      dataset.name / group)
                if not pattern_group_path.exists():
                    _logger.warning(f"Pattern not found: {pattern_group_path}")
                    continue
                patterns = list(pattern_group_path.iterdir())
                if len(patterns) == 0:
                    _logger.warning(f"Pattern not found: {pattern_group_path}")
                    continue
                pattern = patterns[0]
                random.seed(utils.config.get_random_seed())
                buggy_pairs = list(filter(lambda pair: pair.case_path.stem != pattern.stem, pairs))
                buggy_pair = random.choice(buggy_pairs)

                futures.append(executor.submit(detect_bugs,
                                               _data_collection.collection_name, dataset.name, _repos_path,
                                               group, pattern, buggy_pair, _jar_path, timeout_sec))

        # 等待所有任务完成
        concurrent.futures.wait(futures)

    print("All tasks completed.")


if __name__ == "__main__":
    cluster_path = Path("/data/jiangjiajun/LLMPAT/dataset/c3_random_1000")
    repos_path = Path("/data/jiangjiajun/LLMPAT/temporary/repos")
    data_collection = DataCollection(cluster_path, names=None, grouped=True)
    jar_path = ("/data/jiangjiajun/LLMPAT/ModifyMeta/ModifiedMetaModel/artifacts/"
                "ModifiedMetaModel-1.0-SNAPSHOT-runnable.jar")
    # extract pattern
    process(data_collection, repos_path, 50, jar_path, 600.0)
