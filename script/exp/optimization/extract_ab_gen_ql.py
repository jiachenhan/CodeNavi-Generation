import asyncio
import json
import random
from pathlib import Path
from typing import Generator

import utils
from app.abs.mark.inference import Analyzer
from app.communication import PatternInput
from interface.java.run_java_api import java_extract_pattern, java_llm_abstract, java_generate_query
from interface.llm.llm_pool import AsyncLLMPool
from utils.config import set_config, LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def llm_abstract(_llm,
                 _pattern_input: PatternInput,
                 _output_path: Path) -> None:
    try:
        run_llm_analysis(_llm, _pattern_input, _output_path)
    except Exception as e:
        import traceback
        traceback.print_exc()
        _logger.error(f"Error in {_output_path}: {e}")
        return


"""
这个修饰器不支持直接在这个文件作为入口函数时使用
在spawn过程中报错: Can't Pickle <function run_llm_analysis ...>: it's not the same object as __main__.run_llm_analysis
"""
# @timeout(30 * 60)
def run_llm_analysis(_llm,
                     _pattern_input: PatternInput,
                     _output_path: Path):
    analyzer = Analyzer(_llm, _pattern_input, _output_path)
    analyzer.analysis()


def extract_pattern(_jar: str, _case_path: Path, _pattern_path: Path, _pattern_info_path: Path):
    checker_name = _case_path.parent.parent.stem
    group_name = _case_path.parent.stem
    pattern_ori_path = _pattern_path / "ori" / checker_name / group_name / f"{_case_path.stem}.ser"
    pattern_info_input_path = _pattern_info_path / "input" / checker_name / group_name / f"{_case_path.stem}.json"
    java_extract_pattern(30, _case_path, pattern_ori_path, pattern_info_input_path, _jar)


async def async_abstract_pattern(
        _llm_pool: AsyncLLMPool,  # 使用异步池替代单个LLM
        _jar: str,
        _case_path: Path,
        _pattern_path: Path,
        _pattern_info_path: Path
):
    checker_name = _case_path.parent.parent.stem
    group_name = _case_path.parent.stem
    case_info = json.load(open(_case_path / "info.json", 'r'))["may_be_fixed_violations"].strip()

    pattern_ori_path = _pattern_path / "ori" / checker_name / group_name / f"{_case_path.stem}.ser"
    pattern_abs_path = _pattern_path / "abs" / checker_name / group_name / f"{_case_path.stem}.ser"
    pattern_info_input_path = _pattern_info_path / "input" / checker_name / group_name / f"{_case_path.stem}.json"
    pattern_info_output_path = _pattern_info_path / "output" / checker_name / group_name / f"{_case_path.stem}.json"

    if pattern_abs_path.exists():
        return

    pattern_input = PatternInput.from_file(pattern_info_input_path)
    pattern_input.set_error_info(case_info)
    await _llm_pool.async_run(
        llm_abstract,
        pattern_input,
        pattern_info_output_path
    )

    java_llm_abstract(30, pattern_ori_path, pattern_info_output_path, pattern_abs_path, _jar)


def generate_query(_jar: str, _case_path: Path, _pattern_path: Path, _dsl_path: Path):
    checker_name = _case_path.parent.parent.stem
    group_name = _case_path.parent.stem
    pattern_abs_path = _pattern_path / "abs" / checker_name / group_name / f"{_case_path.stem}.ser"
    dsl_output_path = _dsl_path / checker_name / group_name / f"{_case_path.stem}.kirin"

    java_generate_query(30, pattern_abs_path, dsl_output_path, _jar)


def get_ab_case(_pattern_path: Path, _data_path: Path) -> Generator[Path, None, None]:
    _abs_pattern_path = _pattern_path / "ori"
    for abs_pat in _abs_pattern_path.rglob("*.ser"):
        case_name = abs_pat.stem
        group_name = abs_pat.parent.stem
        checker_name = abs_pat.parent.parent.stem
        yield _data_path / checker_name / group_name / case_name


def get_random_code_pair(_path: Path) -> Generator[Path, None, None]:
    for _checker in _path.iterdir():
        if not _checker.is_dir():
            continue
        for group in _checker.iterdir():
            if not group.is_dir():
                continue
            _case_name = random.choice([d for d in group.iterdir() if d.is_dir()])
            case_path = group / _case_name
            yield case_path

async def process_single_case(
        llm_pool: AsyncLLMPool,
        jar: str,
        case: Path,
        pattern_path: Path,
        dsl_path: Path,
        pattern_info_path: Path
):
    extract_pattern(jar, case, pattern_path, pattern_info_path)
    # 异步执行核心步骤
    await async_abstract_pattern(llm_pool, jar, case, pattern_path, pattern_info_path)
    # 同步后续步骤
    generate_query(jar, case, pattern_path, dsl_path)


async def main():
    config = set_config("yunwu")
    jar_path = config.get("jar_path")
    model_name = config.get("openai").get("model")

    llm_pool = AsyncLLMPool([
        (config.get("openai").get("base_url"),
         api_key,
         model_name)
        for api_key in config.get("openai").get("api_keys")
    ])

    # 创建并行任务（限制最大并发数）
    sem = asyncio.Semaphore(5)  # 根据API总限制调整

    dataset_name = "ql"
    dataset_path = Path("E:/dataset/Navi/DEFs") / dataset_name

    pattern_path = utils.config.get_pattern_base_path() / model_name / dataset_name
    pattern_info_path = utils.config.get_pattern_info_base_path() / model_name / dataset_name
    dsl_path = utils.config.get_dsl_base_path() / model_name / dataset_name

    cases = get_random_code_pair(dataset_path)
    # cases = get_ab_case(pattern_path, dataset_path)

    tasks = []
    for case in cases:
        checker_name = case.parent.parent.stem
        group_name = case.parent.stem
        dsl_group_path = dsl_path / checker_name / group_name
        if dsl_group_path.exists():
            _logger.info(f"{checker_name}/{group_name} already exists")
            continue

        task = asyncio.create_task(
            process_single_case(
                llm_pool,
                jar_path,
                case,
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
