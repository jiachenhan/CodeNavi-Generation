import glob
import json
from pathlib import Path
from typing import Tuple, Generator, Optional, Any

from bs4 import BeautifulSoup

"""
    收集毕设第四章的数据集，包括
    1. 待检测函数列表，一次commit的函数 （根据warning list标注true，false）
    2. 将函数构造到单独的文件中，减少扫描的问题 ？？尝试是否可行
"""

debug = False

dataset_names = ["codeql_sampled_v1", "codeql_sampled_v2", "pmd_sampled_v1", "pmd_sampled_v2"]
IGNORED_CHECKERS = ["Expression_always_evaluates_to_the_same_value", "Missing_Override_annotation",
                    "Deprecated_method_or_constructor_invocation",  # codeql
                    "AssignmentInOperand", "JUnit5TestShouldBePackagePrivate", "NullAssignment"]  # pmd


def _get_commit_changed_functions_info(commit_method_info_path: Path) -> list:
    """
        methods.json的结果，也就是一次commit全部changed的函数列表， 也就是全集
        return:
            "file_name": "test/unit/org/apache/cassandra/db/commitlog/CommitLogSegmentManagerCDCTest.java",
            "begin_line": 186,
            "end_line": 224,
            "method_signature": "void testCompletedFlag()#186#224",
            "method_source": "    @Test\n    public void testCompletedFlag() throws IOException\n    {\n...
    """
    with open(commit_method_info_path, 'r', encoding="utf-8") as f:
        methods = json.load(f)
    return methods


def _get_ground_true_warnings(baseline_case_warning_path: Path) -> list:
    """
        这个ground true是blame版本的warning list，也就是tp+fn
        return:
            file: test#unit#org#apache#cassandra#db#commitlog#CommitLogSegmentManagerCDCTest.java
            begin_line: 186
            end_line: 224
    """
    with open(baseline_case_warning_path, 'r', encoding="utf-8") as f:
        warnings = json.load(f)
    return warnings


def find_navi_warnings(xml_file_path: Path):
    """
        return:
            related_file_name: test#unit#org#apache#cassandra#db#commitlog#CommitLogSegmentManagerCDCTest.java
            function_name: testCDCIndexFileWriteOnSync
            report_line: 162
    """

    def reduce_engine_format(s) -> Optional[str]:
        s = s.strip()
        if s.startswith("<![CDATA[") and s.endswith("]]>"):
            return s[9:-3]
        return s

    ret_warnings = []
    with open(xml_file_path, 'r', encoding='ISO-8859-1') as file:
        xml_content = file.read()
    soup = BeautifulSoup(xml_content, 'xml')
    error_tags = soup.find_all("error")
    for error in error_tags:
        detect_info = error.find("defectInfo")
        # print(detect_info.fileName.text.replace("\\", "/"))
        file_name = reduce_engine_format(detect_info.fileName.text)
        related_file_name = file_name.split("\\")[-1]
        function_name = reduce_engine_format(detect_info.function.text) if detect_info.function else None
        report_line = int(detect_info.reportLine.text)
        ret_warnings.append((related_file_name, function_name, report_line))
    return ret_warnings


def matched_warnings(_ground_truth_warning: dict,
                     _navi_warning: tuple[str, str, int]) -> bool:
    """to check whether navi warning matches ground truth warning, navi report line error tolerance +/-1"""
    ground_file_path = _ground_truth_warning["file"]
    ground_begin_line, ground_end_line = _ground_truth_warning["begin_line"], _ground_truth_warning["end_line"]
    navi_related_file_name, navi_function_name, navi_report_line = _navi_warning

    return (navi_related_file_name in ground_file_path
            and ground_begin_line - 1 <= navi_report_line <= ground_end_line + 1)


def collect_and_label_navi_results(
        navi_results: list,
        ground_true_warnings: list,
        commit_changed_functions: list,
        output_path: Path
):
    # 1. 标注 TP/FP
    matched_gt_idx = set()
    labeled_results = []

    def find_func_info(related_file_name, function_name, report_line):
        for func in commit_changed_functions:
            if (related_file_name in func['file_name'].replace("/", "#") and
                    function_name in func['method_signature'] and
                    int(func['begin_line']) - 1 <= int(report_line) <= int(func['end_line']) + 1):
                return func
        return None

    # 标注tp/fp
    for i, navi_warning in enumerate(navi_results):
        navi_file_name, navi_function_name, navi_report_line = navi_warning
        is_tp = False
        matched_gt = None
        for j, gt in enumerate(ground_true_warnings):
            if (navi_file_name in gt["file"] and
                    gt["begin_line"] - 1 <= navi_report_line <= gt["end_line"] + 1):
                is_tp = True
                matched_gt = gt
                matched_gt_idx.add(j)
                break
        func_info = find_func_info(navi_file_name, navi_function_name, navi_report_line)
        if func_info:
            labeled_results.append({
                "file_name": func_info["file_name"],
                "begin_line": func_info["begin_line"],
                "end_line": func_info["end_line"],
                "method_signature": func_info["method_signature"],
                "method_source": func_info["method_source"],
                "label": "tp" if is_tp else "fp",
                "navi_warning": {
                    "related_file_name": navi_file_name,
                    "function_name": navi_function_name,
                    "report_line": navi_report_line
                },
                "ground_truth": matched_gt if is_tp else None
            })

    # 2. 标注 FN
    for j, gt in enumerate(ground_true_warnings):
        if j not in matched_gt_idx:
            # 找到对应的函数信息
            for func in commit_changed_functions:
                if (gt["file"] in func["file_name"].replace("/", "#") and
                        int(func["begin_line"]) - 1 <= int(gt["begin_line"]) <= int(func["end_line"]) + 1):
                    labeled_results.append({
                        "file_name": func["file_name"],
                        "begin_line": func["begin_line"],
                        "end_line": func["end_line"],
                        "method_signature": func["method_signature"],
                        "method_source": func["method_source"],
                        "label": "fn",
                        "navi_warning": None,
                        "ground_truth": gt
                    })
                    break

    # 3. 保存到文件
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(labeled_results, f, ensure_ascii=False, indent=2)


def get_dataset_tool_version(dataset_name: str) -> Tuple[str, str]:
    match dataset_name.split('_'):
        case [tool, version, "commits"] if version in ["v1", "v2"] and tool in ["codeql", "pmd"]:
            return tool, version
        case _:
            raise ValueError(f"Unexpected dataset name format: {dataset_name}")


def main():
    commit_info_path = Path("E:/dataset/Navi/rq2_commit")

    results_base_path = Path(f"E:/dataset/Navi/final_thesis_datas/ori_dsl_detect_results")

    for dataset in commit_info_path.iterdir():
        tool, _ = get_dataset_tool_version(dataset.stem)

        """collect metrics for each dataset"""
        for checker in dataset.iterdir():
            if not checker.is_dir():
                continue
            if checker.stem in IGNORED_CHECKERS:
                continue

            for group in checker.iterdir():
                if not group.is_dir():
                    continue

                scanned_result_dir = results_base_path / dataset.stem / checker.stem / group.stem
                if not scanned_result_dir.exists() or not scanned_result_dir.is_dir():
                    print(f"No scanned result dir in {group}")
                    continue

                scanned_result_path = next(scanned_result_dir.iterdir(), None)
                if not scanned_result_path:
                    print(f"No scanned result in {group}")
                    continue

                query_case_name = scanned_result_path.stem.split("_")[0]
                scanned_case_name = scanned_result_path.stem.split("_")[1]

                """get commit changed functions info"""
                commit_method_info_path = group / scanned_case_name / "methods.json"
                commit_changed_functions = _get_commit_changed_functions_info(commit_method_info_path)

                """get navi results"""
                result_xml_path = (scanned_result_path / "error_report_1.xml")
                if not result_xml_path.exists():
                    print(f"No result xml found in {scanned_result_path}")
                    continue
                navi_results = find_navi_warnings(result_xml_path)

                """get ground true warnings"""

                ground_true_path = group / scanned_case_name / "sat_warnings.json"
                if not ground_true_path.exists():
                    # 和扫描结果对齐，不应该发生
                    print(f"No ground true warning found in {ground_true_path}")
                    continue
                ground_true_results = _get_ground_true_warnings(ground_true_path)

                """collect and label navi results"""
                output_path = (scanned_result_dir / f"{scanned_result_path.stem}_labeled_results.json")
                print(f"Collecting labeled results to {output_path}")
                collect_and_label_navi_results(
                    navi_results,
                    ground_true_results,
                    commit_changed_functions,
                    output_path
                )


if __name__ == "__main__":
    main()
