import asyncio
import json
import random
from pathlib import Path
from typing import Generator

import utils
from exp.pure_llm.gen_dsl import pure_llm_gen
from interface.llm.llm_pool import AsyncLLMPool
from utils.config import set_config


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


async def generate_single(_llm_pool: AsyncLLMPool,
                          _case_path: Path,
                          _dsl_path: Path) -> None:
    case_info = json.load(open(_case_path / "info.json", 'r'))["may_be_fixed_violations"].strip()

    _buggy_path = _case_path / "buggy.java"
    _fixed_path = _case_path / "fixed.java"

    response = await _llm_pool.async_run(
        pure_llm_gen,
        _buggy_path,
        _fixed_path,
        case_info
    )

    if not _dsl_path.exists():
        _dsl_path.parent.mkdir(parents=True, exist_ok=True)

    with open(_dsl_path, 'w') as f:
        f.write(response)


async def main():
    _config = set_config("ppinfra")

    llm_pool = AsyncLLMPool([
        (_config.get("openai").get("base_url"),
         api_key,
         _config.get("openai").get("model"))
        for api_key in _config.get("openai").get("api_keys")
    ])

    sem = asyncio.Semaphore(5)  # 根据API总限制调整

    dataset_name = "codeql_sampled_v2"
    dataset_path = Path("/data/jiangjiajun/CodeNavi-DSL/data") / dataset_name

    dsl_base_path = utils.config.get_dsl_base_path() / "pure_llm" / dataset_name

    cases = get_random_code_pair(dataset_path)

    tasks = []
    for case in cases:
        dsl_path = dsl_base_path / case.parents[1].stem / case.parent.stem / f"{case.stem}.kirin"
        task = asyncio.create_task(
            generate_single(
                llm_pool,
                case,
                dsl_path
            )
        )
        task.add_done_callback(lambda _: sem.release())
        await sem.acquire()
        tasks.append(task)

    await asyncio.gather(*tasks)

if __name__ == "__main__":
    asyncio.run(main())