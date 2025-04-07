import json
from pathlib import Path

from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def collect_result(reports_path: Path) -> list:
    _result = []
    for report_path in reports_path.rglob("*.xml"):
        slice_result = xml_collect_errors(report_path)
        _result.extend(slice_result)
    return _result


def xml_collect_errors(_output_path: Path) -> list:
    _results = []
    if not _output_path.exists():
        return []
    import xml.etree.ElementTree as ET
    try:
        xml_root = ET.parse(_output_path)
    except ET.ParseError:
        _logger.error(f"Parse error: {_output_path}")
        return []
    all_error = xml_root.find("errors").findall("error")
    for error in all_error:
        defect_info = error.find("defectInfo")
        func_name = defect_info.find("function").text
        file_name = defect_info.find("fileName").text.replace("\\", "/")
        report_line = defect_info.find("reportLine").text
        _results.append((report_line, func_name, file_name))
    return _results


if __name__ == "__main__":
    dataset_name = "vul4j"
    # dataset_name = "vul_cvefix"
    results_path = Path(f"E:/dataset/Navi/vul/results/{dataset_name}")

    results = []

    for path_1 in results_path.iterdir():
        for result_path in path_1.iterdir():
            result = collect_result(result_path)
            if result:
                print(f"result_path: {result_path}")
                print(result)
                results.append({"result_path": str(result_path), "result": result})

    result_store_path = results_path / "statistic.json"
    with open(result_store_path, "w", encoding="utf-8") as file:
        json.dump(results, file, indent=4)
