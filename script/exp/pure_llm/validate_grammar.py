from pathlib import Path

from interface.java.run_java_api import kirin_validate


def validate_grammar(_query_base_path: Path) -> bool:
    engine_path = Path("D:/envs/kirin-cli-1.0.8_sp06-jackofext-obfuscate.jar")
    total_query = []
    legal_query = []
    for query_path in _query_base_path.rglob("*.kirin"):
        print(query_path)
        total_query.append(query_path)
        legal_grammar = kirin_validate(5, engine_path, query_path)
        if legal_grammar:
            legal_query.append(query_path)

    print(f"{_query_base_path.stem}:\t total: {len(total_query)}, legal: {len(legal_query)}")
    print(f"ACC: {(len(legal_query)) / (len(total_query))}\n")


if __name__ == '__main__':
    # query_base_path = Path("D:/workspace/CodeNavi-Generation/07dsl/pure_llm/codeql_sampled_v1")
    # query_base_path = Path("D:/workspace/CodeNavi-Generation/07dsl/pure_llm/codeql_sampled_v2")
    # query_base_path = Path("D:/workspace/CodeNavi-Generation/07dsl/pure_llm/pmd_sampled_v1")
    query_base_path = Path("D:/workspace/CodeNavi-Generation/07dsl/pure_llm/pmd_sampled_v2")

    validate_grammar(query_base_path)
