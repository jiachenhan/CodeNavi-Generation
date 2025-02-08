from pathlib import Path

import utils.config
from interface.java.run_java_api import java_generate_query


def generate_query(pattern_path: Path, output_path: Path, jar_path: str):
    java_generate_query(60, pattern_path, output_path, jar_path)


if __name__ == "__main__":
    abstracted_pattern_dir_path = Path("D:/develop_codes/CodeNavi-Generation/01pattern/abs/sample_100_dataset")
    output_query_dir_path = Path("D:/develop_codes/CodeNavi-Generation/07dsl")
    jar_path = utils.config.get_jar_path()
    if not output_query_dir_path.exists():
        output_query_dir_path.mkdir(parents=True, exist_ok=True)

    for file in abstracted_pattern_dir_path.iterdir():
        if file.suffix != ".ser":
            continue
        hash_id = file.stem
        output_file_path = output_query_dir_path / f"{hash_id}.kirin"
        generate_query(file, output_file_path, jar_path)
