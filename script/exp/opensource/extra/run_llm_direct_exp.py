"""
LLM Direct 实验脚本（异步并发版本）

实验内容：
1. 从一个 case 提取 DSL（使用 llm_direct 包的精确匹配逻辑）
2. 在同 group 的另一个 case 上进行检测
3. 统计检测结果 (TP, TN, FP, FN)

使用方式：
    python -m exp.opensource.extra.run_llm_direct_exp
"""
import asyncio
import json
import random
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Generator, List, Tuple, Optional

import utils.config
from app.abs.llm_direct.inference import Analyzer
from app.communication import PatternInput
from interface.java.run_java_api import java_extract_pattern, java_llm_abstract, java_generate_query, kirin_engine
from interface.llm.llm_pool import AsyncLLMPool
from interface.llm.llm_api import LLMAPI
from utils.config import LoggerConfig, set_config, get_random_seed

_logger = LoggerConfig.get_logger(__name__)

# 实验名称，用于区分路径
EXP_NAME = "llm_direct"


class ExperimentConfig:
    """实验配置"""
    def __init__(self, dataset_name: str, model_name: str, jar_path: str):
        self.dataset_name = dataset_name
        self.model_name = model_name
        self.jar_path = jar_path

        # 数据集路径
        self.dataset_path = Path("E:/dataset/Navi/DEFs") / dataset_name

        # 输出路径（在项目根路径下）
        root = utils.config.get_root_project_path()
        self.pattern_path = root / "01pattern" / EXP_NAME / model_name / dataset_name
        self.pattern_info_path = root / "02pattern-info" / EXP_NAME / model_name / dataset_name
        self.dsl_path = root / "07dsl" / EXP_NAME / model_name / dataset_name

        # 检测引擎路径
        self.engine_path = Path("D:/envs/kirin-cli-1.0.8_sp06-jackofext-obfuscate.jar")


def get_all_groups(dataset_path: Path) -> Generator[Tuple[str, str, Path], None, None]:
    """
    遍历数据集，返回所有 (checker, group, group_path)
    """
    for checker in dataset_path.iterdir():
        if not checker.is_dir():
            continue
        for group in checker.iterdir():
            if not group.is_dir():
                continue
            yield checker.stem, group.stem, group


def get_case_pair(group_path: Path) -> Optional[Tuple[Path, Path]]:
    """
    从 group 中选择两个不同的 case：一个用于提取 DSL，一个用于检测
    返回 (extract_case, detect_case)，如果 case 数量不足则返回 None
    """
    cases = [d for d in group_path.iterdir() if d.is_dir()]
    if len(cases) < 2:
        _logger.warning(f"Skipping group {group_path}: less than 2 cases")
        return None

    random.shuffle(cases)
    return cases[0], cases[1]


# ==================== DSL 提取逻辑 ====================

def extract_pattern(case_path: Path, config: ExperimentConfig) -> Path:
    """调用 Java 提取原始 pattern"""
    checker_name = case_path.parent.parent.stem
    group_name = case_path.parent.stem

    pattern_ori_path = config.pattern_path / "ori" / checker_name / group_name / f"{case_path.stem}.ser"
    pattern_info_input_path = config.pattern_info_path / "input" / checker_name / group_name / f"{case_path.stem}.json"

    java_extract_pattern(30, case_path, pattern_ori_path, pattern_info_input_path, config.jar_path)
    return pattern_info_input_path


def abstract_pattern(llm: LLMAPI, case_path: Path, config: ExperimentConfig) -> Path:
    """使用 LLM 抽象 pattern（同步函数，供 AsyncLLMPool 调用）"""
    checker_name = case_path.parent.parent.stem
    group_name = case_path.parent.stem

    # 读取 case info
    info_file = case_path / "info.json"
    if info_file.exists():
        case_info = json.load(open(info_file, 'r'))["may_be_fixed_violations"].strip()
    else:
        case_info = ""

    pattern_ori_path = config.pattern_path / "ori" / checker_name / group_name / f"{case_path.stem}.ser"
    pattern_abs_path = config.pattern_path / "abs" / checker_name / group_name / f"{case_path.stem}.ser"
    pattern_info_input_path = config.pattern_info_path / "input" / checker_name / group_name / f"{case_path.stem}.json"
    pattern_info_output_path = config.pattern_info_path / "output" / checker_name / group_name / f"{case_path.stem}.json"

    # LLM 分析
    pattern_input = PatternInput.from_file(pattern_info_input_path)
    pattern_input.set_error_info(case_info)

    analyzer = Analyzer(llm, pattern_input, pattern_info_output_path)
    analyzer.analysis()

    # Java 抽象
    java_llm_abstract(30, pattern_ori_path, pattern_info_output_path, pattern_abs_path, config.jar_path)
    return pattern_abs_path


def generate_query(case_path: Path, config: ExperimentConfig) -> Path:
    """生成 DSL 查询"""
    checker_name = case_path.parent.parent.stem
    group_name = case_path.parent.stem

    pattern_abs_path = config.pattern_path / "abs" / checker_name / group_name / f"{case_path.stem}.ser"
    dsl_output_path = config.dsl_path / checker_name / group_name / f"{case_path.stem}.kirin"

    java_generate_query(30, pattern_abs_path, dsl_output_path, config.jar_path)
    return dsl_output_path


# ==================== 检测逻辑 ====================

def run_detection(dsl_path: Path, detect_case_path: Path, config: ExperimentConfig) -> Tuple[Path, Path]:
    """
    使用 DSL 检测 case
    返回 (buggy_output_path, fixed_output_path)
    """
    buggy_file = detect_case_path / "buggy.java"
    fixed_file = detect_case_path / "fixed.java"

    # 输出路径放在 DSL 所在目录下
    output_base = dsl_path.parent
    buggy_output_path = output_base / "scan_error_output"
    fixed_output_path = output_base / "scan_correct_output"

    if buggy_file.exists():
        kirin_engine(60.0, config.engine_path, dsl_path, buggy_file, buggy_output_path)
    if fixed_file.exists():
        kirin_engine(60.0, config.engine_path, dsl_path, fixed_file, fixed_output_path)

    return buggy_output_path, fixed_output_path


# ==================== 异步处理单个 group ====================

async def process_single_group(
    llm_pool: AsyncLLMPool,
    extract_case: Path,
    detect_case: Path,
    config: ExperimentConfig
):
    """异步处理单个 group：提取 DSL + 检测"""
    checker_name = extract_case.parent.parent.stem
    group_name = extract_case.parent.stem

    _logger.info(f"Processing: {checker_name}/{group_name}")
    _logger.info(f"  Extract case: {extract_case.stem}")
    _logger.info(f"  Detect case: {detect_case.stem}")

    # 1. 提取原始 pattern（同步，不需要 LLM）
    extract_pattern(extract_case, config)

    # 2. LLM 抽象（异步，通过 llm_pool）
    await llm_pool.async_run(abstract_pattern, extract_case, config)

    # 3. 生成 DSL（同步，不需要 LLM）
    dsl_path = generate_query(extract_case, config)
    _logger.info(f"  DSL generated: {dsl_path}")

    # 4. 检测（同步，不需要 LLM）
    run_detection(dsl_path, detect_case, config)
    _logger.info(f"  Detection completed: {checker_name}/{group_name}")


# ==================== 统计逻辑 ====================

def xml_check_has_error(output_path: Path) -> bool:
    """检查 XML 报告中是否有错误"""
    report_path = output_path / "error_report_1.xml"
    if not report_path.exists():
        return False

    try:
        xml_root = ET.parse(report_path)
        all_error = xml_root.find("errors").findall("error")
        return bool(all_error)
    except Exception as e:
        _logger.warning(f"Failed to parse XML: {report_path}, error: {e}")
        return False


def collect_results(config: ExperimentConfig) -> dict:
    """收集所有检测结果"""
    tp, tn, fp, fn = [], [], [], []

    if not config.dsl_path.exists():
        return {"tp": tp, "tn": tn, "fp": fp, "fn": fn}

    for checker in config.dsl_path.iterdir():
        if not checker.is_dir():
            continue
        for group in checker.iterdir():
            if not group.is_dir():
                continue

            buggy_output_path = group / "scan_error_output"
            fixed_output_path = group / "scan_correct_output"

            # 检测 buggy 文件
            if xml_check_has_error(buggy_output_path):
                tp.append(group)
            else:
                fn.append(group)

            # 检测 fixed 文件
            if xml_check_has_error(fixed_output_path):
                fp.append(group)
            else:
                tn.append(group)

    return {"tp": tp, "tn": tn, "fp": fp, "fn": fn}


def print_statistics(results: dict, dataset_name: str):
    """打印统计结果"""
    tp, tn, fp, fn = len(results["tp"]), len(results["tn"]), len(results["fp"]), len(results["fn"])
    total = tp + tn + fp + fn

    print(f"\n{'='*50}")
    print(f"Dataset: {dataset_name}")
    print(f"{'='*50}")
    print(f"TP: {tp}, TN: {tn}, FP: {fp}, FN: {fn}")

    if total > 0:
        acc = (tp + tn) / total
        print(f"ACC: {acc:.4f}")

    if tp + fp > 0:
        precision = tp / (tp + fp)
        print(f"Precision: {precision:.4f}")

    if tp + fn > 0:
        recall = tp / (tp + fn)
        print(f"Recall: {recall:.4f}")

    if tp + fp > 0 and tp + fn > 0:
        precision = tp / (tp + fp)
        recall = tp / (tp + fn)
        if precision + recall > 0:
            f1 = 2 * precision * recall / (precision + recall)
            print(f"F1: {f1:.4f}")

    # FPR = FP / (FP + TN)
    if fp + tn > 0:
        fpr = fp / (fp + tn)
        print(f"FPR: {fpr:.4f}")

    # FNR = FN / (FN + TP)
    if fn + tp > 0:
        fnr = fn / (fn + tp)
        print(f"FNR: {fnr:.4f}")

    print(f"{'='*50}\n")


# ==================== 主函数 ====================

async def run_experiment(dataset_name: str):
    """异步运行单个数据集的实验"""
    random.seed(get_random_seed())

    # 配置
    config_dict = set_config("yunwu2")
    jar_path = config_dict.get("jar_path")
    model_name = config_dict.get("openai").get("model")

    # 创建 LLM 池
    llm_pool = AsyncLLMPool([
        (config_dict.get("openai").get("base_url"),
         api_key,
         model_name)
        for api_key in config_dict.get("openai").get("api_keys")
    ])

    exp_config = ExperimentConfig(dataset_name, model_name, jar_path)

    _logger.info(f"Starting experiment for dataset: {dataset_name}")
    _logger.info(f"Model: {model_name}")
    _logger.info(f"Dataset path: {exp_config.dataset_path}")
    _logger.info(f"DSL output path: {exp_config.dsl_path}")
    _logger.info(f"LLM pool size: {len(llm_pool.clients)}")

    # 收集所有需要处理的 group
    tasks_info = []
    for checker_name, group_name, group_path in get_all_groups(exp_config.dataset_path):
        # 检查是否已处理
        dsl_group_path = exp_config.dsl_path / checker_name / group_name
        if dsl_group_path.exists():
            _logger.info(f"Skipping {checker_name}/{group_name}: already processed")
            continue

        # 获取 case pair
        case_pair = get_case_pair(group_path)
        if case_pair is None:
            continue

        extract_case, detect_case = case_pair
        tasks_info.append((extract_case, detect_case))

    _logger.info(f"Total groups to process: {len(tasks_info)}")

    # 创建并发控制信号量（根据 LLM 池大小）
    sem = asyncio.Semaphore(len(llm_pool.clients))

    async def bounded_process(extract_case: Path, detect_case: Path):
        """带信号量控制的处理函数"""
        async with sem:
            try:
                await process_single_group(llm_pool, extract_case, detect_case, exp_config)
            except Exception as e:
                checker_name = extract_case.parent.parent.stem
                group_name = extract_case.parent.stem
                _logger.error(f"Error processing {checker_name}/{group_name}: {e}")

    # 创建所有任务
    tasks = [
        asyncio.create_task(bounded_process(extract_case, detect_case))
        for extract_case, detect_case in tasks_info
    ]

    # 等待所有任务完成
    if tasks:
        await asyncio.gather(*tasks)

    # 统计结果
    results = collect_results(exp_config)
    print_statistics(results, dataset_name)

    return results


async def main():
    """主入口"""
    datasets = ["ql", "pmd"]
    all_results = {}

    for dataset_name in datasets:
        _logger.info(f"\n{'#'*60}")
        _logger.info(f"# Running experiment for: {dataset_name}")
        _logger.info(f"{'#'*60}\n")

        results = await run_experiment(dataset_name)
        all_results[dataset_name] = results

    # 打印汇总
    print("\n" + "="*60)
    print("SUMMARY")
    print("="*60)
    for dataset_name, results in all_results.items():
        print_statistics(results, dataset_name)


if __name__ == "__main__":
    asyncio.run(main())
