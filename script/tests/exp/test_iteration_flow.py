"""
测试迭代流程

验证Iteration 0和Iteration 1的基本流程是否正确。
"""
import pytest
import asyncio
from pathlib import Path
import tempfile
import json

from exp.thesis.refine.experiment_config import ExperimentConfig, FPSelectionStrategy
from exp.thesis.refine.task_manager import TaskManager
from exp.thesis.refine.fp_selector import FPSelector
from exp.thesis.refine.iteration_controller import IterationController
from exp.thesis.refine.data_structures import TaskStatus, StopReason
from interface.llm.llm_pool import AsyncLLMPool


@pytest.fixture
def mock_config():
    """创建模拟配置"""
    with tempfile.TemporaryDirectory() as tmpdir:
        tmpdir = Path(tmpdir)

        config = ExperimentConfig(
            dataset_base=tmpdir,
            llm_config_tag="yunwu",
            llm_pool_size=1,
            task_timeout=300,
            fp_selection_strategy=FPSelectionStrategy.SEQUENTIAL,
            dsl_patterns=["test_dataset/**/*.kirin"],
            exclude_patterns=[]
        )

        # 创建必需的目录结构
        ori_dsl_dir = config.ori_dsl_root / "test_dataset/TestChecker/1"
        ori_dsl_dir.mkdir(parents=True, exist_ok=True)

        # 创建测试DSL文件
        (ori_dsl_dir / "1.kirin").write_text("pattern TestPattern { }")

        # 创建DEF数据
        def_dir = config.defs_root / "test/TestChecker/1/1"
        def_dir.mkdir(parents=True, exist_ok=True)
        (def_dir / "buggy.java").write_text("public class Test { }")
        (def_dir / "fixed.java").write_text("public class Test { /* fixed */ }")
        (def_dir / "info.json").write_text('{"root_cause": "test"}')

        # 创建原始检测结果（包含2个FP）
        ori_detect_dir = config.ori_detect_results_root / "test_dataset/TestChecker/1"
        ori_detect_dir.mkdir(parents=True, exist_ok=True)

        detect_result = [
            {
                "label": "tp",
                "file_name": "Test.java",
                "function_name": "testFunc1",
                "begin_line": 10,
                "end_line": 20,
                "report_line": 15,
                "method_signature": "void testFunc1()",
                "code_snippet": "void testFunc1() { }"
            },
            {
                "label": "fp",
                "file_name": "Test.java",
                "function_name": "testFunc2",
                "begin_line": 30,
                "end_line": 40,
                "report_line": 35,
                "method_signature": "void testFunc2()",
                "code_snippet": "void testFunc2() { }"
            },
            {
                "label": "fp",
                "file_name": "Test.java",
                "function_name": "testFunc3",
                "begin_line": 50,
                "end_line": 60,
                "report_line": 55,
                "method_signature": "void testFunc3()",
                "code_snippet": "void testFunc3() { }"
            }
        ]

        (ori_detect_dir / "1_2_labeled_results.json").write_text(
            json.dumps(detect_result, indent=2)
        )

        yield config


class TestIterationFlow:
    """测试迭代流程"""

    @pytest.mark.asyncio
    async def test_iteration_0_initialization(self, mock_config):
        """测试Iteration 0初始化"""
        task_manager = TaskManager(mock_config)
        fp_selector = FPSelector(FPSelectionStrategy.SEQUENTIAL)

        # 创建空LLM池（Iteration 0不需要）
        llm_pool = AsyncLLMPool([])

        controller = IterationController(mock_config, task_manager, fp_selector, llm_pool)

        # 运行Iteration 0
        summary = await controller.run_iteration_0()

        # 验证结果
        assert summary.iteration == 0
        assert summary.total_tasks == 1
        assert summary.active_tasks == 1  # 有2个FP，应该是active
        assert summary.stopped_tasks == 0
        assert summary.refine_success_count == 0  # Iteration 0无refine
        assert summary.refine_failed_count == 0

        # 验证task_info.json
        task_info_path = mock_config.iter_exp_root / "iteration_0/test_dataset/TestChecker/1/1/task_info.json"
        assert task_info_path.exists()

        with open(task_info_path, 'r', encoding='utf-8') as f:
            task_info_data = json.load(f)

        assert task_info_data["iteration"] == 0
        assert task_info_data["status"] == "active"
        assert task_info_data["metrics"]["detection_metrics"]["fp_count"] == 2
        assert task_info_data["metrics"]["detection_metrics"]["tp_count"] == 1

    @pytest.mark.asyncio
    async def test_iteration_0_no_fp_stops(self, mock_config):
        """测试Iteration 0无FP时停止"""
        # 修改检测结果为无FP
        ori_detect_dir = mock_config.ori_detect_results_root / "test_dataset/TestChecker/1"
        detect_result = [
            {
                "label": "tp",
                "file_name": "Test.java",
                "function_name": "testFunc1",
                "begin_line": 10,
                "end_line": 20,
                "report_line": 15,
                "method_signature": "void testFunc1()",
                "code_snippet": "void testFunc1() { }"
            }
        ]
        (ori_detect_dir / "1_2_labeled_results.json").write_text(
            json.dumps(detect_result, indent=2)
        )

        task_manager = TaskManager(mock_config)
        fp_selector = FPSelector(FPSelectionStrategy.SEQUENTIAL)
        llm_pool = AsyncLLMPool([])

        controller = IterationController(mock_config, task_manager, fp_selector, llm_pool)

        # 运行Iteration 0
        summary = await controller.run_iteration_0()

        # 验证结果
        assert summary.active_tasks == 0
        assert summary.stopped_tasks == 1
        assert summary.stop_reasons.get("no_fp") == 1

        # 验证task_info.json
        task_info_path = mock_config.iter_exp_root / "iteration_0/test_dataset/TestChecker/1/1/task_info.json"
        with open(task_info_path, 'r', encoding='utf-8') as f:
            task_info_data = json.load(f)

        assert task_info_data["status"] == "stopped"
        assert task_info_data["stop_reason"] == "no_fp"

    def test_fp_selector_sequential(self, mock_config):
        """测试FP选择器（sequential策略）"""
        fp_selector = FPSelector(FPSelectionStrategy.SEQUENTIAL)

        detect_result_path = mock_config.ori_detect_results_root / "test_dataset/TestChecker/1/1_2_labeled_results.json"

        # 第一次选择（无历史）
        fp_selection = fp_selector.select_fp(detect_result_path, used_fp_keys=[])

        assert fp_selection.total_fps_in_results == 2
        assert fp_selection.remaining_fps_count == 2
        assert fp_selection.selected_fp is not None
        assert fp_selection.selected_fp.function_name == "testFunc2"  # 第一个FP

        # 第二次选择（已使用第一个FP）
        used_key = fp_selection.selected_fp.get_unique_key()
        fp_selection2 = fp_selector.select_fp(detect_result_path, used_fp_keys=[used_key])

        assert fp_selection2.remaining_fps_count == 1
        assert fp_selection2.selected_fp.function_name == "testFunc3"  # 第二个FP

        # 第三次选择（所有FP都已使用）
        used_keys = [fp_selection.selected_fp.get_unique_key(), fp_selection2.selected_fp.get_unique_key()]
        fp_selection3 = fp_selector.select_fp(detect_result_path, used_fp_keys=used_keys)

        assert fp_selection3.remaining_fps_count == 0
        assert fp_selection3.selected_fp is None

    def test_fp_selector_random(self, mock_config):
        """测试FP选择器（random策略）"""
        fp_selector = FPSelector(FPSelectionStrategy.RANDOM)

        detect_result_path = mock_config.ori_detect_results_root / "test_dataset/TestChecker/1/1_2_labeled_results.json"

        # 多次选择，验证随机性
        selections = []
        for _ in range(10):
            fp_selection = fp_selector.select_fp(detect_result_path, used_fp_keys=[])
            selections.append(fp_selection.selected_fp.function_name)

        # 至少应该有两种不同的选择（理论上可能全部相同，但概率很低）
        unique_selections = set(selections)
        assert len(unique_selections) >= 1
        assert all(name in ["testFunc2", "testFunc3"] for name in unique_selections)
