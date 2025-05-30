from pathlib import Path
from typing import Generator

import utils.config
from interface.java.run_java_api import java_generate_query


def generate_query(_jar: str, _pattern_abs_path: Path, _dsl_path: Path):
    case_name = _pattern_abs_path.stem
    group_name = _pattern_abs_path.parent.stem
    checker_name = _pattern_abs_path.parent.parent.stem

    dsl_output_path = _dsl_path / checker_name / group_name / f"{case_name}.kirin"
    java_generate_query(30, _pattern_abs_path, dsl_output_path, _jar)


def get_abstract_pattern(_pattern_path: Path) -> Generator[Path, None, None]:
    _abs_pattern_path = _pattern_path / "abs"
    yield from _abs_pattern_path.rglob("*.ser")


if __name__ == "__main__":
    _config = utils.config.set_config("aliyun")
    jar_path = _config.get("jar_path")
    dataset_name = "pmd_sampled_v1"
    dataset_path = Path("/data/jiangjiajun/CodeNavi-DSL/data") / dataset_name

    pattern_path = utils.config.get_pattern_base_path() / dataset_name
    dsl_path = utils.config.get_dsl_base_path() / "no_logic" / dataset_name

    for _pattern_path in get_abstract_pattern(pattern_path):
        generate_query(jar_path, _pattern_path, dsl_path)
