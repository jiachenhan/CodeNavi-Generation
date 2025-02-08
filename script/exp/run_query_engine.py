from pathlib import Path
import xml.etree.ElementTree as ET

from interface.java.run_java_api import kirin_engine


def check_detect_result(_query_base_path: Path):
    parse_error = []
    check_null = []
    queries = _query_base_path.rglob("*.kirin")
    query_num = sum(1 for _ in queries)
    for query in _query_base_path.rglob("*.kirin"):
        print(query)
        output_path = query.parent / f"{query.stem}_output" / "error_report_1.xml"
        if not output_path.exists():
            parse_error.append(query)
            continue

        xml_root = ET.parse(output_path)
        all_error = xml_root.find("errors").findall("error")
        if not all_error:
            check_null.append(query)
            continue

        print(f"all query: {query_num}, parse_error: {len(parse_error)}, check_null: {len(check_null)}")

        if parse_error:
            print("parser error:")
            for error in parse_error:
                print(error)

        if check_null:
            print("check null:")
            for error in check_null:
                print(error)


def run_query(_query_base_path: Path):
    test_input_base_path = Path("D:/datas/DSLtestinput")
    engine_path = Path("D:/env/kirin-cli-1.0.8_sp06-jackofall.jar")

    queries = _query_base_path.rglob("*.kirin")
    for query in queries:
        print(query)
        relative_path = query.parent.parent.relative_to(_query_base_path)
        scanned_file = test_input_base_path / relative_path / f"{query.parent.stem}.java"
        output_path = _query_base_path / relative_path / query.parent.stem / f"{query.stem}_output"
        kirin_engine(60.0, engine_path, query, scanned_file, output_path)


if __name__ == "__main__":
    query_base_path = Path("D:/workspace/CodeNavi-Generation/ModifiedMetaModel/04query")
    # run_query(query_base_path)
    check_detect_result(query_base_path)