"""
测试实验配置模块
"""
import pytest
from pathlib import Path

from exp.thesis.refine.experiment_config import (
    ExperimentConfig,
    FPSelectionStrategy,
    get_default_config
)


class TestFPSelectionStrategy:
    def test_strategy_values(self):
        assert FPSelectionStrategy.SEQUENTIAL.value == "sequential"
        assert FPSelectionStrategy.RANDOM.value == "random"


class TestExperimentConfig:
    def test_default_config(self):
        config = ExperimentConfig()

        assert config.dataset_base == Path("E:/dataset/Navi")
        assert config.ori_dsl_root == Path("E:/dataset/Navi/final_thesis_datas/ori_dsl")
        assert config.llm_config_tag == "yunwu"
        assert config.llm_pool_size == 5
        assert config.task_timeout == 300
        assert config.fp_selection_strategy == FPSelectionStrategy.SEQUENTIAL

    def test_custom_paths(self):
        custom_base = Path("/custom/path")
        config = ExperimentConfig(dataset_base=custom_base)

        assert config.dataset_base == custom_base
        assert config.ori_dsl_root == custom_base / "final_thesis_datas/ori_dsl"
        assert config.defs_root == custom_base / "DEFs"

    def test_custom_llm_config(self):
        config = ExperimentConfig(
            llm_config_tag="aliyun",
            llm_pool_size=10,
            task_timeout=600
        )

        assert config.llm_config_tag == "aliyun"
        assert config.llm_pool_size == 10
        assert config.task_timeout == 600

    def test_custom_dsl_patterns(self):
        config = ExperimentConfig(
            dsl_patterns=["pmd_v1_commits/**/*.kirin"],
            exclude_patterns=["**/excluded/**"]
        )

        assert len(config.dsl_patterns) == 1
        assert config.dsl_patterns[0] == "pmd_v1_commits/**/*.kirin"
        assert config.exclude_patterns == ["**/excluded/**"]

    def test_get_default_config(self):
        config = get_default_config()
        assert isinstance(config, ExperimentConfig)
        assert config.llm_config_tag == "yunwu"

    def test_get_llm_config(self):
        config = ExperimentConfig()
        llm_config = config.get_llm_config()

        assert llm_config is not None
        assert "openai" in llm_config
        assert "api_keys" in llm_config["openai"]
        assert "base_url" in llm_config["openai"]
        assert "model" in llm_config["openai"]
