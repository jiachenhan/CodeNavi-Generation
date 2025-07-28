import asyncio
import json
import random
from pathlib import Path
from typing import Generator

import utils
from app.communication import PatternInput
from app.pipeline.abstract import navi_abstract
from interface.java.run_java_api import java_extract_pattern, java_llm_abstract, java_generate_query
from interface.llm.llm_pool import AsyncLLMPool
from utils.config import set_config, LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def extract_pattern(_jar: str, _group_path: Path, _pattern_path: Path, _pattern_info_path: Path):
    pattern_ori_path = _pattern_path / "ori" / f"{_group_path.stem}.ser"
    pattern_info_input_path = _pattern_info_path / "input" / f"{_group_path.stem}.json"
    java_extract_pattern(30, _group_path, pattern_ori_path, pattern_info_input_path, _jar)


async def async_abstract_pattern(
        _llm_pool: AsyncLLMPool,  # 使用异步池替代单个LLM
        _jar: str,
        _group_path: Path,
        _pattern_path: Path,
        _pattern_info_path: Path
):
    error_info = json.load(open(_group_path / "info.json", 'r'))["issue"].strip()

    pattern_ori_path = _pattern_path / "ori" / f"{_group_path.stem}.ser"
    pattern_abs_path = _pattern_path / "abs" / f"{_group_path.stem}.ser"
    pattern_info_input_path = _pattern_info_path / "input" / f"{_group_path.stem}.json"
    pattern_info_output_path = _pattern_info_path / "output" / f"{_group_path.stem}.json"

    if pattern_abs_path.exists():
        return

    pattern_input = PatternInput.from_file(pattern_info_input_path)
    pattern_input.set_error_info(error_info)
    await _llm_pool.async_run(
        navi_abstract,
        pattern_input,
        pattern_info_output_path
    )

    java_llm_abstract(30, pattern_ori_path, pattern_info_output_path, pattern_abs_path, _jar)


def generate_query(_jar: str, _group_path: Path, _pattern_path: Path, _dsl_path: Path):
    pattern_abs_path = _pattern_path / "abs" / f"{_group_path.stem}.ser"
    dsl_output_path = _dsl_path / f"{_group_path.stem}.kirin"

    java_generate_query(30, pattern_abs_path, dsl_output_path, _jar)


def get_ab_case(_pattern_path: Path, _data_path: Path) -> Generator[Path, None, None]:
    _abs_pattern_path = _pattern_path / "ori"
    for abs_pat in _abs_pattern_path.rglob("*.ser"):
        case_name = abs_pat.stem
        group_name = abs_pat.parent.stem
        checker_name = abs_pat.parent.parent.stem
        yield _data_path / checker_name / group_name / case_name


def get_code_pairs(_path: Path) -> Generator[Path, None, None]:
    for group in _path.iterdir():
        if not group.is_dir():
            continue
        yield group


async def process_single_case(
        llm_pool: AsyncLLMPool,
        jar: str,
        group: Path,
        pattern_path: Path,
        dsl_path: Path,
        pattern_info_path: Path
):
    extract_pattern(jar, group, pattern_path, pattern_info_path)
    # 异步执行核心步骤
    await async_abstract_pattern(llm_pool, jar, group, pattern_path, pattern_info_path)
    # 同步后续步骤
    generate_query(jar, group, pattern_path, dsl_path)


async def main():
    _config = set_config("ppinfra")
    jar_path = _config.get("jar_path")

    llm_pool = AsyncLLMPool([
        (_config.get("openai").get("base_url"),
         api_key,
         _config.get("openai").get("model"))
        for api_key in _config.get("openai").get("api_keys")
    ])

    # 创建并行任务（限制最大并发数）
    sem = asyncio.Semaphore(5)  # 根据API总限制调整

    dataset_name = "vul4j"
    dataset_path = Path("/data/jiangjiajun/CodeNavi-DSL/data") / dataset_name

    pattern_path = utils.config.get_pattern_base_path() / dataset_name
    pattern_info_path = utils.config.get_pattern_info_base_path() / dataset_name
    dsl_path = utils.config.get_dsl_base_path() / dataset_name

    groups = get_code_pairs(dataset_path)

    tasks = []
    for group in groups:
        group_name = group.stem

        dsl_group_path = dsl_path / group_name
        if dsl_group_path.exists():
            _logger.info(f"{str(dsl_group_path)} already exists")
            continue

        task = asyncio.create_task(
            process_single_case(
                llm_pool,
                jar_path,
                group,
                pattern_path,
                dsl_path,
                pattern_info_path
            )
        )
        task.add_done_callback(lambda _: sem.release())
        await sem.acquire()
        tasks.append(task)

    await asyncio.gather(*tasks)

if __name__ == "__main__":
    asyncio.run(main())
