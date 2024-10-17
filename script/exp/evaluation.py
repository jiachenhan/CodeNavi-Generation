import re
from typing import List

import utils.config
from data.dataset import OneMethodFilePair, DataCollection
from interface.java.run_java_api import java_gain_oracle


def check_success(_data_collection: DataCollection, java_program: str):
    for dataset in _data_collection.datasets:
        total = 0
        adapted = 0
        success = 0
        failed = []
        for group, pairs in dataset.get_datas():
            total += 1
            pattern_pair: OneMethodFilePair = pairs[0]
            buggy_pairs: List[OneMethodFilePair] = pairs[1:]
            patch_base_path = (utils.config.get_patches_base_path() / _data_collection.collection_name
                               / dataset.name / group)

            correct_flag = False
            for buggy_pair in buggy_pairs:
                if correct_flag:
                    break

                fixed_path = buggy_pair.after_path
                patch_path = patch_base_path / f"{pattern_pair.case_path.stem}-{buggy_pair.case_path.stem}"

                if patch_path.exists():
                    oracle_path = patch_path / "oracle.txt"
                    if not oracle_path.exists():
                        java_gain_oracle(60.0, fixed_path, "null", oracle_path, java_program)

                    try:
                        oracle = re.sub(r"\s+", "", "class PlaceHold {" + oracle_path.read_text() + "}")
                    except FileNotFoundError:
                        continue

                    adapted += 1
                    for patch in patch_path.glob("*.java"):
                        patched = re.sub(r"\s+", "", patch.read_text())
                        if oracle == patched:
                            success += 1
                            correct_flag = True
                            break
            if not correct_flag:
                failed.append(group)

        print(f"Dataset: {dataset.name}")
        print(f"Total: {total}")
        print(f"Adapted: {adapted}")
        print(f"Success: {success}")
        # print(f"Failed: {failed}")
