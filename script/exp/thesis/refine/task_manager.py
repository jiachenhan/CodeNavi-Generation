"""
任务管理器模块

负责扫描DSL文件、创建任务、加载输入数据、管理任务状态。
"""
import shutil
from pathlib import Path
from typing import List, Optional

from exp.thesis.refine.experiment_config import ExperimentConfig
from exp.thesis.refine.path_utils import PathMapper
from exp.thesis.refine.data_structures import (
    RefinerTask, TaskInfo, TaskStatus, StopReason,
    TaskMetrics, DetectionMetrics, get_current_timestamp
)
from exp.thesis.refine.utils import infer_scanned_case_for_task
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


class TaskManager:
    """任务管理器"""

    def __init__(self, config: ExperimentConfig):
        """
        初始化任务管理器

        Args:
            config: 实验配置
        """
        self.config = config
        self.path_mapper = PathMapper(config)
        _logger.info("Initialized TaskManager")

    def scan_dsl_tasks(self) -> List[str]:
        """
        扫描所有符合条件的DSL任务

        Returns:
            任务ID列表
        """
        task_ids = self.path_mapper.scan_dsl_files()
        _logger.info(f"Scanned {len(task_ids)} DSL tasks")
        return task_ids

    def initialize_iteration_0(self, task_id: str) -> Optional[TaskInfo]:
        """
        初始化Iteration 0任务（复制原始DSL，加载原始检测结果）

        Args:
            task_id: 任务ID

        Returns:
            TaskInfo对象（如果初始化成功）
        """
        _logger.info(f"Initializing iteration 0 for task: {task_id}")

        # 自动推断scanned_case
        scanned_case = infer_scanned_case_for_task(task_id, 0, self.path_mapper)
        if scanned_case is None:
            _logger.warning(f"Task {task_id}: Failed to infer scanned_case, skipping")
            return None

        # 验证输入路径
        if not self.path_mapper.validate_input_paths(task_id, scanned_case):
            _logger.error(f"Task {task_id}: Missing required input files")
            return None

        # 获取路径
        task_paths = self.path_mapper.get_task_paths(task_id, scanned_case)
        output_dir = self.path_mapper.get_iteration_output_dir(task_id, 0)
        output_dir.mkdir(parents=True, exist_ok=True)

        # 复制原始DSL
        output_dsl_path = output_dir / "dsl.kirin"
        shutil.copy(task_paths.ori_dsl_path, output_dsl_path)
        _logger.info(f"Copied original DSL to {output_dsl_path}")

        # 读取原始检测结果
        ori_detect_result_path = task_paths.ori_detect_result_path
        if not ori_detect_result_path.exists():
            _logger.warning(f"Original detect result not found: {ori_detect_result_path}")
            # 没有检测结果，无法计算metrics，但可以继续（标记为无FP）
            detection_metrics = DetectionMetrics.from_counts(tp=0, fp=0, fn=0)
            status = TaskStatus.STOPPED
            stop_reason = StopReason.NO_FP
        else:
            # 计算detection_metrics
            detection_metrics = self._compute_detection_metrics(ori_detect_result_path)

            # 判断是否有FP
            if detection_metrics.fp_count == 0:
                _logger.info(f"Task {task_id}: No FPs in iteration 0, marking as stopped")
                status = TaskStatus.STOPPED
                stop_reason = StopReason.NO_FP
            else:
                _logger.info(f"Task {task_id}: Found {detection_metrics.fp_count} FPs in iteration 0")
                status = TaskStatus.ACTIVE
                stop_reason = None

        # 创建TaskInfo
        task_info = TaskInfo(
            task_id=task_id,
            iteration=0,
            status=status,
            metrics=TaskMetrics(
                refine_metrics=None,  # Iteration 0无refine
                detection_metrics=detection_metrics
            ),
            timestamp=get_current_timestamp(),
            stop_reason=stop_reason,
            stopped_at=0 if stop_reason else None
        )

        # 保存task_info.json
        task_info_path = output_dir / "task_info.json"
        task_info.save_to_file(task_info_path)
        _logger.info(f"Saved task_info.json to {task_info_path}")

        return task_info

    def load_task_info(self, task_id: str, iteration: int) -> Optional[TaskInfo]:
        """
        加载指定迭代的task_info.json

        Args:
            task_id: 任务ID
            iteration: 迭代轮次

        Returns:
            TaskInfo对象（如果文件存在）
        """
        task_info_path = self.path_mapper.get_iteration_task_info_path(task_id, iteration)

        if not task_info_path.exists():
            _logger.warning(f"Task info not found: {task_info_path}")
            return None

        try:
            task_info = TaskInfo.load_from_file(task_info_path)
            return task_info
        except Exception as e:
            _logger.error(f"Failed to load task info from {task_info_path}: {e}")
            return None

    def create_refiner_task(
        self,
        task_id: str,
        current_iteration: int,
        prev_task_info: TaskInfo,
        fp_info,  # FPInfo
        scanned_case: str
    ) -> RefinerTask:
        """
        创建RefinerTask（用于执行refine）

        Args:
            task_id: 任务ID
            current_iteration: 当前迭代轮次
            prev_task_info: 上一轮的TaskInfo
            fp_info: 本轮选择的FP信息
            scanned_case: 扫描的case编号（由调用方推断）

        Returns:
            RefinerTask对象
        """
        # 获取路径
        task_paths = self.path_mapper.get_task_paths(task_id, scanned_case)
        prev_dsl_path = self.path_mapper.get_iteration_dsl_path(task_id, current_iteration - 1)
        output_dir = self.path_mapper.get_iteration_output_dir(task_id, current_iteration)

        refiner_task = RefinerTask(
            task_id=task_id,
            iteration=current_iteration,
            dsl_path=prev_dsl_path,
            output_dir=output_dir,
            status=TaskStatus.ACTIVE,
            buggy_path=task_paths.buggy_path,
            fixed_path=task_paths.fixed_path,
            root_cause_path=task_paths.info_path,
            fp_info=fp_info
        )

        return refiner_task

    def _compute_detection_metrics(self, detect_result_path: Path) -> DetectionMetrics:
        """
        从检测结果文件计算detection_metrics

        Args:
            detect_result_path: 检测结果文件路径（*_labeled_results.json）

        Returns:
            DetectionMetrics对象
        """
        import json

        with open(detect_result_path, 'r', encoding='utf-8') as f:
            detect_results = json.load(f)

        tp_count = sum(1 for r in detect_results if r.get("label") == "tp")
        fp_count = sum(1 for r in detect_results if r.get("label") == "fp")
        fn_count = sum(1 for r in detect_results if r.get("label") == "fn")

        return DetectionMetrics.from_counts(tp=tp_count, fp=fp_count, fn=fn_count)
