"""
测试实验数据结构模块
"""
import pytest
from pathlib import Path
import tempfile
import json

from exp.thesis.refine.data_structures import (
    TaskStatus,
    StopReason,
    FPInfo,
    FPSelection,
    DetectionMetrics,
    RefineMetrics,
    TaskMetrics,
    TaskInfo,
    RefinerTask,
    IterationSummary,
    get_current_timestamp
)


class TestEnums:
    """测试枚举类型"""

    def test_task_status(self):
        """测试任务状态枚举"""
        assert TaskStatus.ACTIVE.value == "active"
        assert TaskStatus.STOPPED.value == "stopped"

    def test_stop_reason(self):
        """测试停止原因枚举"""
        assert StopReason.NO_FP.value == "no_fp"
        assert StopReason.NO_FP_CONVERGED.value == "no_fp_converged"
        assert StopReason.ALL_FPS_EXHAUSTED.value == "all_fps_exhausted"
        assert StopReason.REFINE_FAILED.value == "refine_failed"


class TestFPInfo:
    """测试FP信息数据结构"""

    def test_fpinfo_creation(self):
        """测试FPInfo创建"""
        fp = FPInfo(
            label="fp",
            file_name="Test.java",
            function_name="testMethod",
            begin_line=10,
            end_line=20,
            report_line=15,
            method_signature="public void testMethod()",
            code_snippet="public void testMethod() { ... }"
        )

        assert fp.label == "fp"
        assert fp.file_name == "Test.java"
        assert fp.function_name == "testMethod"

    def test_fpinfo_unique_key(self):
        """测试FP唯一标识"""
        fp1 = FPInfo(
            label="fp",
            file_name="Test.java",
            function_name="testMethod",
            begin_line=10,
            end_line=20,
            report_line=15,
            method_signature="public void testMethod()",
            code_snippet="..."
        )

        fp2 = FPInfo(
            label="fp",
            file_name="Test.java",
            function_name="testMethod",
            begin_line=10,
            end_line=20,
            report_line=15,
            method_signature="public void testMethod()",
            code_snippet="different snippet"  # 代码片段不同
        )

        # 相同位置的FP应该有相同的唯一键
        assert fp1.get_unique_key() == fp2.get_unique_key()

    def test_fpinfo_dict_conversion(self):
        """测试FPInfo字典转换"""
        fp = FPInfo(
            label="fp",
            file_name="Test.java",
            function_name="testMethod",
            begin_line=10,
            end_line=20,
            report_line=15,
            method_signature="public void testMethod()",
            code_snippet="..."
        )

        # 转换为字典
        fp_dict = fp.to_dict()
        assert fp_dict["file_name"] == "Test.java"

        # 从字典创建
        fp_restored = FPInfo.from_dict(fp_dict)
        assert fp_restored.file_name == fp.file_name
        assert fp_restored.function_name == fp.function_name

    def test_fpinfo_from_detection_result(self):
        """测试从检测结果创建FPInfo"""
        detection_result = {
            "label": "fp",
            "file_name": "Foo.java",
            "function_name": "bar",
            "begin_line": 42,
            "end_line": 55,
            "report_line": 45,
            "method_signature": "public void bar()",
            "code_snippet": "public void bar() { ... }"
        }

        fp = FPInfo.from_detection_result(detection_result)
        assert fp.file_name == "Foo.java"
        assert fp.function_name == "bar"
        assert fp.report_line == 45


class TestDetectionMetrics:
    """测试检测指标数据结构"""

    def test_detection_metrics_from_counts(self):
        """测试从TP/FP/FN计数创建检测指标"""
        metrics = DetectionMetrics.from_counts(tp=20, fp=10, fn=3)

        assert metrics.tp_count == 20
        assert metrics.fp_count == 10
        assert metrics.fn_count == 3
        assert metrics.total_warnings == 30
        assert abs(metrics.precision - 0.6667) < 0.001
        assert abs(metrics.recall - 0.8696) < 0.001
        assert metrics.f1_score > 0

    def test_detection_metrics_zero_division(self):
        """测试零除处理"""
        metrics = DetectionMetrics.from_counts(tp=0, fp=0, fn=0)

        assert metrics.precision == 0.0
        assert metrics.recall == 0.0
        assert metrics.f1_score == 0.0

    def test_detection_metrics_dict_conversion(self):
        """测试检测指标字典转换"""
        metrics = DetectionMetrics.from_counts(tp=20, fp=10, fn=3)

        metrics_dict = metrics.to_dict()
        assert "precision" in metrics_dict
        assert "recall" in metrics_dict

        restored = DetectionMetrics.from_dict(metrics_dict)
        assert restored.precision == metrics.precision


class TestFPSelection:
    """测试FP选择信息数据结构"""

    def test_fp_selection_with_selected_fp(self):
        """测试包含选中FP的选择信息"""
        fp = FPInfo(
            label="fp",
            file_name="Test.java",
            function_name="test",
            begin_line=1,
            end_line=10,
            report_line=5,
            method_signature="void test()",
            code_snippet="..."
        )

        selection = FPSelection(
            total_fps_in_results=10,
            remaining_fps_count=9,
            selected_fp=fp
        )

        assert selection.total_fps_in_results == 10
        assert selection.remaining_fps_count == 9
        assert selection.selected_fp is not None

    def test_fp_selection_dict_conversion(self):
        """测试FP选择字典转换"""
        fp = FPInfo.from_detection_result({
            "label": "fp",
            "file_name": "Test.java",
            "function_name": "test",
            "begin_line": 1,
            "end_line": 10,
            "report_line": 5,
            "method_signature": "void test()",
            "code_snippet": "..."
        })

        selection = FPSelection(
            total_fps_in_results=10,
            remaining_fps_count=9,
            selected_fp=fp
        )

        selection_dict = selection.to_dict()
        assert selection_dict["total_fps_in_results"] == 10

        restored = FPSelection.from_dict(selection_dict)
        assert restored.selected_fp.file_name == "Test.java"


class TestTaskInfo:
    """测试任务信息数据结构"""

    def test_task_info_iteration_0(self):
        """测试Iteration 0的任务信息（无FP选择）"""
        detection_metrics = DetectionMetrics.from_counts(tp=20, fp=10, fn=3)
        metrics = TaskMetrics(
            refine_metrics=None,
            detection_metrics=detection_metrics
        )

        task_info = TaskInfo(
            task_id="pmd_v1_commits/AvoidInstanceof/3/2",
            iteration=0,
            status=TaskStatus.ACTIVE,
            metrics=metrics,
            timestamp="2024-01-21 09:00:00"
        )

        assert task_info.iteration == 0
        assert task_info.fp_selection is None
        assert len(task_info.fp_history) == 0

    def test_task_info_iteration_n(self):
        """测试Iteration N的任务信息（包含FP选择和历史）"""
        fp = FPInfo.from_detection_result({
            "label": "fp",
            "file_name": "Test.java",
            "function_name": "test",
            "begin_line": 1,
            "end_line": 10,
            "report_line": 5,
            "method_signature": "void test()",
            "code_snippet": "..."
        })

        fp_selection = FPSelection(
            total_fps_in_results=10,
            remaining_fps_count=9,
            selected_fp=fp
        )

        refine_metrics = RefineMetrics(
            refine_success=True,
            refine_time_seconds=45.2,
            prompt_tokens=5234,
            completion_tokens=892,
            total_tokens=6126
        )

        detection_metrics = DetectionMetrics.from_counts(tp=18, fp=7, fn=2)

        metrics = TaskMetrics(
            refine_metrics=refine_metrics,
            detection_metrics=detection_metrics
        )

        task_info = TaskInfo(
            task_id="pmd_v1_commits/AvoidInstanceof/3/2",
            iteration=2,
            status=TaskStatus.ACTIVE,
            metrics=metrics,
            timestamp="2024-01-21 10:00:00",
            fp_selection=fp_selection,
            fp_history={
                "iteration_1": fp
            }
        )

        assert task_info.iteration == 2
        assert task_info.fp_selection is not None
        assert "iteration_1" in task_info.fp_history

    def test_task_info_stopped(self):
        """测试停止状态的任务信息"""
        detection_metrics = DetectionMetrics.from_counts(tp=20, fp=0, fn=0)
        metrics = TaskMetrics(
            refine_metrics=None,
            detection_metrics=detection_metrics
        )

        task_info = TaskInfo(
            task_id="test/task",
            iteration=3,
            status=TaskStatus.STOPPED,
            metrics=metrics,
            timestamp="2024-01-21 10:00:00",
            stop_reason=StopReason.NO_FP_CONVERGED,
            stopped_at=3
        )

        assert task_info.status == TaskStatus.STOPPED
        assert task_info.stop_reason == StopReason.NO_FP_CONVERGED
        assert task_info.stopped_at == 3

    def test_task_info_file_io(self):
        """测试任务信息文件IO"""
        with tempfile.TemporaryDirectory() as tmpdir:
            file_path = Path(tmpdir) / "task_info.json"

            # 创建任务信息
            detection_metrics = DetectionMetrics.from_counts(tp=20, fp=10, fn=3)
            metrics = TaskMetrics(
                refine_metrics=None,
                detection_metrics=detection_metrics
            )

            task_info = TaskInfo(
                task_id="test/task",
                iteration=0,
                status=TaskStatus.ACTIVE,
                metrics=metrics,
                timestamp="2024-01-21 09:00:00"
            )

            # 保存到文件
            task_info.save_to_file(file_path)
            assert file_path.exists()

            # 从文件加载
            loaded = TaskInfo.load_from_file(file_path)
            assert loaded.task_id == task_info.task_id
            assert loaded.iteration == task_info.iteration
            assert loaded.status == task_info.status


class TestIterationSummary:
    """测试迭代汇总数据结构"""

    def test_iteration_summary_creation(self):
        """测试迭代汇总创建"""
        summary = IterationSummary(
            iteration=1,
            total_tasks=100,
            active_tasks=85,
            stopped_tasks=15,
            refine_success_count=82,
            refine_failed_count=3,
            total_time_seconds=1234.5,
            total_tokens=520000,
            stop_reasons={
                "no_fp": 10,
                "refine_failed": 3,
                "all_fps_exhausted": 2
            }
        )

        assert summary.iteration == 1
        assert summary.total_tasks == 100
        assert summary.active_tasks == 85

    def test_iteration_summary_file_io(self):
        """测试迭代汇总文件IO"""
        with tempfile.TemporaryDirectory() as tmpdir:
            file_path = Path(tmpdir) / "iteration_summary.json"

            summary = IterationSummary(
                iteration=1,
                total_tasks=100,
                active_tasks=85,
                stopped_tasks=15,
                refine_success_count=82,
                refine_failed_count=3,
                total_time_seconds=1234.5,
                total_tokens=520000,
                stop_reasons={"no_fp": 10}
            )

            # 保存
            summary.save_to_file(file_path)
            assert file_path.exists()

            # 加载
            loaded = IterationSummary.load_from_file(file_path)
            assert loaded.iteration == summary.iteration
            assert loaded.total_tasks == summary.total_tasks


class TestHelperFunctions:
    """测试辅助函数"""

    def test_get_current_timestamp(self):
        """测试获取当前时间戳"""
        timestamp = get_current_timestamp()
        assert isinstance(timestamp, str)
        assert len(timestamp) == 19  # "YYYY-MM-DD HH:MM:SS" format
        assert timestamp[4] == "-"
        assert timestamp[10] == " "
