import random
from pathlib import Path
from typing import Generator

from interface.java.run_java_api import genpat_detect


def get_code_pair(_path: Path, _case_name) -> Generator[Path, None, None]:
    for group in _path.iterdir():
        if not group.is_dir():
            continue
        case_path = group / _case_name
        yield case_path

if __name__ == '__main__':
    dataset_name = "codeql_hiera_60"
    dataset_path = Path("/data/jiangjiajun/CodeNavi-DSL/data") / dataset_name

    genpat_cmd = Path("/data/jiangjiajun/CodeNavi-DSL/GenPat")
    genpat_jar = genpat_cmd / "GenPat-1.0-SNAPSHOT-runnable.jar"

    cases = get_code_pair(dataset_path, "0")

    tp = []
    tn = []
    fp = []
    fn = []

    for pattern_case in cases:
        _sub_case_path = [d for d in pattern_case.parent.iterdir() if d.is_dir() and d.stem != pattern_case.stem]

        _pattern_buggy_path = pattern_case / "buggy.java"
        _pattern_fixed_path = pattern_case / "fixed.java"

        _random_case_path = random.choice(_sub_case_path)
        _test_buggy_path = _random_case_path / "buggy.java"
        _test_fixed_path = _random_case_path / "fixed.java"

        detect_buggy = genpat_detect(30,
                                     _pattern_buggy_path, _pattern_fixed_path, _test_buggy_path,
                                     genpat_jar)

        detect_fixed = genpat_detect(30,
                                     _pattern_buggy_path, _pattern_fixed_path, _test_fixed_path,
                                     genpat_jar)

        if detect_buggy:
            tp.append(pattern_case)
        else:
            fn.append(pattern_case)

        if detect_fixed:
            fp.append(pattern_case)
        else:
            tn.append(pattern_case)


    print(f"{dataset_name}:\t tp: {len(tp)}, tn: {len(tn)}, fp: {len(fp)}, fn: {len(fn)}")

    print(f"ACC: {(len(tp) + len(tn)) / (len(tp) + len(tn) + len(fp) + len(fn))}\n"
          f"PRE: {(len(tp)) / (len(tp) + len(fp))}\n"
          f"RECALL: {(len(tp)) / (len(tp) + len(fn))}\n")


