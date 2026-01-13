import glob
import json
from pathlib import Path
from typing import Tuple, Generator, Optional

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


def reduce_navi_warning_notin_changed_functions(results: list,
                                                commit_changed_functions: list) -> list:
    """
    过滤results，只保留属于commit_changed_functions的warning
    line error tolerance +/-1
    """

    def is_in_changed_functions_navi(warning):
        related_file_name, function_name, report_line = warning
        for func in commit_changed_functions:
            if (related_file_name in func['file_name'].replace("/", "#") and
                    function_name in func['method_signature'] and
                    int(func['begin_line']) - 1 <= int(report_line) <= int(func['end_line']) + 1):
                return True
        return False

    return [w for w in results if is_in_changed_functions_navi(w)]


def _get_navi_detect_metrics(navi_results: list,
                             ground_true_warnings: list,
                             commit_changed_functions: list
                             ) -> dict:
    """compare navi results with ground true warnings to get tp, fp, tn, fn"""
    # 过滤
    filtered_navi_results = reduce_navi_warning_notin_changed_functions(navi_results,
                                                                        commit_changed_functions)

    # 匹配
    matched = set()
    for i, navi in enumerate(filtered_navi_results):
        for j, gt in enumerate(ground_true_warnings):
            if matched_warnings(gt, navi):
                matched.add((i, j))
                break

    tp = len(matched)
    fp = len(filtered_navi_results) - tp
    fn = len(ground_true_warnings) - tp

    total = len(commit_changed_functions)
    tn = total - tp - fp - fn

    return {"tp": tp, "fp": fp, "tn": tn, "fn": fn}


def calculate_total_metrics(all_metrics: list) -> dict:
    """calculate total metrics from all metrics"""
    total = {"tp": 0, "fp": 0, "tn": 0, "fn": 0}
    for m in all_metrics:
        total["tp"] += m["tp"]
        total["fp"] += m["fp"]
        total["tn"] += m["tn"]
        total["fn"] += m["fn"]

    tp, fp, tn, fn = total["tp"], total["fp"], total["tn"], total["fn"]
    total_count = tp + fp + tn + fn
    accuracy = (tp + tn) / total_count if total_count > 0 else 0
    precision = tp / (tp + fp) if (tp + fp) > 0 else 0
    recall = tp / (tp + fn) if (tp + fn) > 0 else 0
    f1 = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0
    fpr = fp / (fp + tn) if (fp + tn) > 0 else 0
    fnr = fn / (fn + tp) if (fn + tp) > 0 else 0

    return {
        "tp": tp, "fp": fp, "tn": tn, "fn": fn,
        "accuracy": round(accuracy, 4),
        "precision": round(precision, 4),
        "recall": round(recall, 4),
        "f1": round(f1, 4),
        "fpr": round(fpr, 4),
        "fnr": round(fnr, 4)
    }


def get_dataset_tool_version(dataset_name: str) -> Tuple[str, str]:
    match dataset_name.split('_'):
        case [tool, version, "commits"] if version in ["v1", "v2"] and tool in ["codeql", "pmd"]:
            return tool, version
        case _:
            raise ValueError(f"Unexpected dataset name format: {dataset_name}")


def main():
    commit_info_path = Path("E:/dataset/Navi/rq2_commit")

    results_base_path = Path(f"E:/dataset/Navi/final_thesis_datas/ori_dsl_detect_results")

    ql_metrics = []
    pmd_metrics = []
    for dataset in commit_info_path.iterdir():
        tool, _ = get_dataset_tool_version(dataset.stem)

        """collect metrics for each dataset"""
        all_metrics = []
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
                    # print(f"No scanned result dir in {group}")
                    continue

                scanned_result_path = next(scanned_result_dir.iterdir(), None)
                if not scanned_result_path:
                    # print(f"No scanned result in {group}")
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
                    # 和扫描结果对其，不应该发生
                    print(f"No ground true warning found in {ground_true_path}")
                    continue
                ground_true_results = _get_ground_true_warnings(ground_true_path)

                """compare navi results with ground true warnings"""
                metrics = _get_navi_detect_metrics(navi_results, ground_true_results, commit_changed_functions)
                if debug:
                    print("xml path: ", result_xml_path)
                    print("Metrics:", metrics)

                all_metrics.append(metrics)
                if tool == "codeql":
                    ql_metrics.append(metrics)
                elif tool == "pmd":
                    pmd_metrics.append(metrics)

        sub_dataset_metrics = calculate_total_metrics(all_metrics)
        print(f"Sub {dataset.stem} metrics:", sub_dataset_metrics)

    total_ql_metrics = calculate_total_metrics(ql_metrics)
    total_pmd_metrics = calculate_total_metrics(pmd_metrics)
    print("Total CodeQL metrics:", total_ql_metrics)
    print("Total PMD metrics:", total_pmd_metrics)


if __name__ == "__main__":
    main()
