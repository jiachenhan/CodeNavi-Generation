import random
import concurrent.futures
from pathlib import Path
from typing import List

import utils
from data.dataset import DataCollection, OneMethodFilePair
from interface.java.run_java_api import java_genpat_detect, java_detect, java_abstract
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def abstract_pat(_origin_pattern_path: Path,
                 _abstract_info_path: Path,
                 _abstract_pattern_path: Path,
                 _jar_path: str,
                 timeout_sec: float):
    _logger.info(f"Starting abstract pattern for {_origin_pattern_path.stem}")
    if not _origin_pattern_path.exists() or not _abstract_info_path.exists():
        _logger.error(f"Invalid origin pattern path: {_origin_pattern_path}")
        return
    java_abstract(timeout_sec, _origin_pattern_path, _abstract_info_path, _abstract_pattern_path, _jar_path)


def process(_origin_pattern_path: Path,
            _max_threads: int,
            _jar_path: str,
            timeout_sec: float):
    with concurrent.futures.ThreadPoolExecutor(max_workers=_max_threads) as executor:
        futures = []
        for dataset in _origin_pattern_path.iterdir():
            for group in dataset.iterdir():
                for origin_pattern in group.iterdir():
                    case_name = origin_pattern.stem

                    abstract_info_path = (utils.config.get_pattern_info_base_path() / "output" /
                                          _origin_pattern_path.stem / dataset.stem / group.stem / f"{case_name}.json")
                    abs_pattern_path = (utils.config.get_pattern_base_path() / "abs" /
                                        _origin_pattern_path.stem / dataset.stem / group.stem / f"{case_name}.ser")
                futures.append(executor.submit(abstract_pat,
                                               origin_pattern, abstract_info_path, abs_pattern_path,
                                               _jar_path, timeout_sec))
        # 等待所有任务完成
        concurrent.futures.wait(futures)
    print("All tasks completed.")


if __name__ == "__main__":
    origin_patterns_path = Path("/data/jiangjiajun/LLMPAT/ModifyMeta/01pattern/ori/c3_random_1000")
    jar_path = ("/data/jiangjiajun/LLMPAT/ModifyMeta/ModifiedMetaModel/artifacts/"
                "ModifiedMetaModel-1.0-SNAPSHOT-runnable.jar")
    # extract pattern
    process(origin_patterns_path, 20, jar_path, 180.0)
