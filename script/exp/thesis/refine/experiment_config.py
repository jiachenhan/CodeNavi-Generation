"""
实验配置管理模块
"""
from pathlib import Path
from dataclasses import dataclass, field
from typing import List, Dict
from enum import Enum

from utils.config import set_config


class FPSelectionStrategy(Enum):
    SEQUENTIAL = "sequential"
    RANDOM = "random"


@dataclass
class ExperimentConfig:
    dataset_base: Path = Path("E:/dataset/Navi")

    ori_dsl_root: Path = None
    ori_detect_results_root: Path = None
    defs_root: Path = None
    commit_root: Path = None
    iter_exp_root: Path = None
    final_dsl_root: Path = None  # 最终版本DSL目录
    rq3_exp_root: Path = None    # RQ3实验结果目录

    llm_config_tag: str = "yunwu"
    llm_pool_size: int = 5
    task_timeout: int = 300

    fp_selection_strategy: FPSelectionStrategy = FPSelectionStrategy.SEQUENTIAL

    dsl_patterns: List[str] = field(default_factory=lambda: [
        "pmd_v1_commits/**/*.kirin",
        "pmd_v2_commits/**/*.kirin",
        "codeql_v1_commits/**/*.kirin",
        "codeql_v2_commits/**/*.kirin"
    ])

    exclude_patterns: List[str] = field(default_factory=list)

    def __post_init__(self):
        self.dataset_base = Path(self.dataset_base)

        self.ori_dsl_root = self.ori_dsl_root or self.dataset_base / "final_thesis_datas/ori_dsl"
        self.ori_detect_results_root = self.ori_detect_results_root or self.dataset_base / "final_thesis_datas/ori_dsl_detect_results"
        self.defs_root = self.defs_root or self.dataset_base / "DEFs"
        self.commit_root = self.commit_root or self.dataset_base / "rq2_commit"
        self.iter_exp_root = self.iter_exp_root or self.dataset_base / "final_thesis_datas/iterExp"
        self.final_dsl_root = self.final_dsl_root or self.dataset_base / "final_thesis_datas/final_version"
        self.rq3_exp_root = self.rq3_exp_root or self.dataset_base / "final_thesis_datas/rq3_exp"

    def get_llm_config(self) -> Dict:
        config = set_config(self.llm_config_tag)
        if config is None:
            raise ValueError(f"Invalid llm_config_tag: {self.llm_config_tag}")
        return config


def get_default_config() -> ExperimentConfig:
    return ExperimentConfig()
