import random
from pathlib import Path
import xml.etree.ElementTree as ET

from interface.java.run_java_api import kirin_engine


def xml_check_has_error(_output_path: Path) -> bool:
    if not _output_path.exists():
        return False

    xml_root = ET.parse(_output_path)
    all_error = xml_root.find("errors").findall("error")
    return bool(all_error)


def check_detect_result(_query_base_path: Path):
    tp = []
    tn = []
    fp = []
    fn = []
    no_result = []

    query_num = sum(1 for _ in _query_base_path.rglob("*.kirin"))
    for checker in _query_base_path.iterdir():
        for group in checker.iterdir():
            buggy_output_path = group / "scan_self_error_output" / "error_report_1.xml"
            fixed_output_path = group / "scan_self_correct_output" / "error_report_1.xml"

            if xml_check_has_error(buggy_output_path):
                tp.append(group)
            else:
                fn.append(group)

            if xml_check_has_error(fixed_output_path):
                fp.append(group)
            else:
                tn.append(group)


    print(f"{_query_base_path.stem}:\t tp: {len(tp)}, tn: {len(tn)}, fp: {len(fp)}, fn: {len(fn)}")

    print(f"ACC: {(len(tp) + len(tn)) / (len(tp) + len(tn) + len(fp) + len(fn))}\n"
          f"PRE: {(len(tp)) / (len(tp) + len(fp))}\n"
          f"RECALL: {(len(tp)) / (len(tp) + len(fn))}\n")


def run_query(_query_base_path: Path, _dataset_path: Path):
    engine_path = Path("D:/envs/kirin-cli-1.0.8_sp06-jackofext-obfuscate.jar")

    for checker in _query_base_path.iterdir():
        for group in checker.iterdir():
            dsl_case = next(group.glob("*.kirin"))

            target_group_path = _dataset_path / checker.stem / group.stem
            _sub_case_paths = [d for d in target_group_path.iterdir() if d.is_dir() and d.stem == dsl_case.stem]

            _random_case_path = _sub_case_paths[0]

            buggy_case_path = _random_case_path / "buggy.java"
            fixed_case_path = _random_case_path / "fixed.java"

            buggy_output_path = group / "scan_self_error_output"
            fixed_output_path = group / "scan_self_correct_output"

            kirin_engine(60.0, engine_path, dsl_case, buggy_case_path, buggy_output_path)
            kirin_engine(60.0, engine_path, dsl_case, fixed_case_path, fixed_output_path)


if __name__ == '__main__':
    dataset_name = "codeql_sampled_v2"
    query_base_path = Path(f"D:/workspace/CodeNavi-Generation/07dsl/3-21-all-sampled/{dataset_name}")
    dataset_path = Path(f"E:/dataset/Navi/3-23-sampled-datasets/{dataset_name}")

    run_query(query_base_path, dataset_path)
    check_detect_result(query_base_path)
