import asyncio
import json
from pathlib import Path

import utils
from app.communication import PatternInput
from app.refine.zero_shot_refine import Refiner
from interface.llm.llm_api import LLMAPI
from interface.llm.llm_pool import AsyncLLMPool
from utils.config import set_config


def load_input_pattern(code_pair_path: Path, pattern_input_path: Path) -> PatternInput:
    case_info = json.load(open(code_pair_path / "info.json", 'r'))["may_be_fixed_violations"].strip()
    pattern_input = PatternInput.from_file(pattern_input_path)
    pattern_input.set_error_info(case_info)
    return pattern_input


def refine_llm(llm: LLMAPI,
               origin_dsl_path: Path,
               pattern_input: PatternInput,
               log_path: Path,
               refine_dsl_path: Path):
    refiner = Refiner(llm, pattern_input, log_path)
    with open(origin_dsl_path, "r") as ori_f:
        ori_dsl = ori_f.read()
        refined_dsl = refiner.refine(ori_dsl)

    with open(refine_dsl_path, "w") as refined_f:
        refined_f.write(refined_dsl)


async def refine_single_case(
        llm_pool: AsyncLLMPool,
        origin_dsl_path: Path,
        code_pair_path: Path,
        pattern_input_path: Path,
        log_path: Path,
        refine_dsl_path: Path
):
    pattern_input = load_input_pattern(code_pair_path, pattern_input_path)
    await llm_pool.async_run(
        refine_llm,
        origin_dsl_path,
        pattern_input,
        log_path,
        refine_dsl_path
    )


async def main():
    config = set_config("yunwu")
    model_name = config.get("openai").get("model")

    llm_pool = AsyncLLMPool([
        (config.get("openai").get("base_url"),
         api_key,
         model_name)
        for api_key in config.get("openai").get("api_keys")
    ])

    # 创建并行任务（限制最大并发数）
    sem = asyncio.Semaphore(5)  # 根据API总限制调整

    dataset_name = "pmd"
    dataset_path = Path("E:/dataset/Navi/DEFs") / dataset_name

    pattern_info_path = utils.config.get_pattern_info_base_path() / model_name / dataset_name
    dsl_path = utils.config.get_dsl_base_path() / model_name / dataset_name

    tasks = []
    for ori_dsl_path in dsl_path.rglob("*.kirin"):
        if ori_dsl_path.name.startswith("refined"):
            continue  # 跳过以refined开头的文件
        case_name = ori_dsl_path.stem
        group_name = ori_dsl_path.parent.stem
        checker_name = ori_dsl_path.parent.parent.stem

        code_pair_path = dataset_path / checker_name / group_name / case_name
        pattern_input_path = pattern_info_path / "input" / checker_name / group_name / f"{case_name}.json"
        log_path = ori_dsl_path.parent / "refine.log"
        refined_dsl_path = ori_dsl_path.parent / f"refined_{case_name}.kirin"

        task = asyncio.create_task(
            refine_single_case(llm_pool, ori_dsl_path, code_pair_path, pattern_input_path, log_path, refined_dsl_path)
        )
        task.add_done_callback(lambda _: sem.release())
        await sem.acquire()
        tasks.append(task)

    await asyncio.gather(*tasks)

if __name__ == "__main__":
    asyncio.run(main())
