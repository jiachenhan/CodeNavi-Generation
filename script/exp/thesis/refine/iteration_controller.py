"""
迭代控制器模块

负责管理多轮迭代流程、判断任务状态、协调FP选择和refine执行。
"""
import asyncio
import time
from pathlib import Path
from typing import List, Dict, Optional

from exp.thesis.refine.experiment_config import ExperimentConfig
from exp.thesis.refine.path_utils import PathMapper
from exp.thesis.refine.task_manager import TaskManager
from exp.thesis.refine.fp_selector import FPSelector
from exp.thesis.refine.data_structures import (
    TaskInfo, TaskStatus, StopReason, RefinerTask,
    TaskMetrics, RefineMetrics, DetectionMetrics,
    IterationSummary, get_current_timestamp
)
from exp.thesis.refine.utils import infer_scanned_case_for_task
from interface.llm.llm_pool import AsyncLLMPool
from interface.llm.llm_api import LLMAPI
from app.refine.dsl_refiner import DSLRefiner
from app.refine.data_structures import RefineInput
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


class IterationController:
    """迭代控制器"""

    def __init__(
        self,
        config: ExperimentConfig,
        task_manager: TaskManager,
        fp_selector: FPSelector,
        llm_pool: AsyncLLMPool
    ):
        """
        初始化迭代控制器

        Args:
            config: 实验配置
            task_manager: 任务管理器
            fp_selector: FP选择器
            llm_pool: LLM池
        """
        self.config = config
        self.task_manager = task_manager
        self.fp_selector = fp_selector
        self.llm_pool = llm_pool
        self.path_mapper = PathMapper(config)
        _logger.info("Initialized IterationController")

    async def run_iteration_0(self) -> IterationSummary:
        """
        运行Iteration 0（初始化阶段）

        Returns:
            IterationSummary对象
        """
        _logger.info("=" * 80)
        _logger.info("Running Iteration 0 (Initialization)")
        _logger.info("=" * 80)

        start_time = time.time()

        # 扫描所有DSL任务
        task_ids = self.task_manager.scan_dsl_tasks()
        _logger.info(f"Found {len(task_ids)} DSL tasks")

        # 初始化每个任务
        task_infos: List[TaskInfo] = []
        for task_id in task_ids:
            task_info = self.task_manager.initialize_iteration_0(task_id)
            if task_info:
                task_infos.append(task_info)

        # 统计结果
        total_tasks = len(task_infos)
        active_tasks = sum(1 for t in task_infos if t.status == TaskStatus.ACTIVE)
        stopped_tasks = sum(1 for t in task_infos if t.status == TaskStatus.STOPPED)

        stop_reasons: Dict[str, int] = {}
        for task_info in task_infos:
            if task_info.stop_reason:
                reason_key = task_info.stop_reason.value
                stop_reasons[reason_key] = stop_reasons.get(reason_key, 0) + 1

        elapsed_time = time.time() - start_time

        summary = IterationSummary(
            iteration=0,
            total_tasks=total_tasks,
            active_tasks=active_tasks,
            stopped_tasks=stopped_tasks,
            refine_success_count=0,  # Iteration 0无refine
            refine_failed_count=0,
            total_time_seconds=round(elapsed_time, 2),
            total_tokens=0,
            stop_reasons=stop_reasons
        )

        # 保存汇总
        summary_path = self.path_mapper.get_iteration_summary_path(0)
        summary.save_to_file(summary_path)
        _logger.info(f"Saved iteration summary to {summary_path}")

        _logger.info(f"Iteration 0 completed: {active_tasks} active, {stopped_tasks} stopped")
        return summary

    async def run_iteration_n(self, iteration: int) -> IterationSummary:
        """
        运行Iteration N (N >= 1)（优化迭代阶段）

        Args:
            iteration: 迭代轮次

        Returns:
            IterationSummary对象
        """
        _logger.info("=" * 80)
        _logger.info(f"Running Iteration {iteration} (Optimization)")
        _logger.info("=" * 80)

        start_time = time.time()

        # 加载上一轮任务状态
        prev_iteration = iteration - 1
        task_ids = self.task_manager.scan_dsl_tasks()
        _logger.info(f"Found {len(task_ids)} DSL tasks")

        # Step 1: 选择FP并创建RefinerTask
        refiner_tasks: List[RefinerTask] = []
        for task_id in task_ids:
            prev_task_info = self.task_manager.load_task_info(task_id, prev_iteration)

            if not prev_task_info:
                _logger.warning(f"Task {task_id}: No task_info found for iteration {prev_iteration}, skipping")
                continue

            if prev_task_info.status == TaskStatus.STOPPED:
                _logger.info(f"Task {task_id}: Already stopped, skipping")
                continue

            # 自动推断scanned_case
            scanned_case = infer_scanned_case_for_task(task_id, prev_iteration, self.path_mapper)
            if scanned_case is None:
                _logger.warning(f"Task {task_id}: Failed to infer scanned_case, skipping")
                continue

            # 选择FP
            detect_result_path = self._get_detect_result_path(task_id, prev_iteration, scanned_case)
            used_fp_keys = self.fp_selector.get_used_fp_keys(prev_task_info.fp_history)
            fp_selection = self.fp_selector.select_fp(detect_result_path, used_fp_keys)

            # 判断是否可以继续
            if fp_selection.selected_fp is None:
                # 无FP可用，标记停止
                if fp_selection.total_fps_in_results == 0:
                    stop_reason = StopReason.NO_FP_CONVERGED
                else:
                    stop_reason = StopReason.ALL_FPS_EXHAUSTED

                stopped_task_info = self._create_stopped_task_info(
                    prev_task_info, iteration, stop_reason
                )
                self._save_task_info(stopped_task_info, task_id, iteration)
                _logger.info(f"Task {task_id}: Stopped due to {stop_reason.value}")
                continue

            # 创建RefinerTask
            refiner_task = self.task_manager.create_refiner_task(
                task_id, iteration, prev_task_info, fp_selection.selected_fp, scanned_case
            )
            refiner_task.fp_selection = fp_selection  # 附加FP选择信息
            refiner_task.prev_task_info = prev_task_info  # 附加上一轮信息
            refiner_tasks.append(refiner_task)

        _logger.info(f"Created {len(refiner_tasks)} active refiner tasks")

        # Step 2: 异步执行refine
        if refiner_tasks:
            results = await self._execute_refine_batch(refiner_tasks, iteration)
        else:
            results = []

        # Step 3: 统计结果
        total_tasks = len(task_ids)
        refine_success_count = sum(1 for r in results if r.get("success", False))
        refine_failed_count = sum(1 for r in results if not r.get("success", False))
        total_tokens = sum(r.get("total_tokens", 0) for r in results)

        # 统计停止原因
        stop_reasons: Dict[str, int] = {}
        active_count = 0
        stopped_count = 0

        for task_id in task_ids:
            task_info = self.task_manager.load_task_info(task_id, iteration)
            if task_info:
                if task_info.status == TaskStatus.ACTIVE:
                    active_count += 1
                else:
                    stopped_count += 1
                    if task_info.stop_reason:
                        reason_key = task_info.stop_reason.value
                        stop_reasons[reason_key] = stop_reasons.get(reason_key, 0) + 1

        elapsed_time = time.time() - start_time

        summary = IterationSummary(
            iteration=iteration,
            total_tasks=total_tasks,
            active_tasks=active_count,
            stopped_tasks=stopped_count,
            refine_success_count=refine_success_count,
            refine_failed_count=refine_failed_count,
            total_time_seconds=round(elapsed_time, 2),
            total_tokens=total_tokens,
            stop_reasons=stop_reasons
        )

        # 保存汇总
        summary_path = self.path_mapper.get_iteration_summary_path(iteration)
        summary.save_to_file(summary_path)
        _logger.info(f"Saved iteration summary to {summary_path}")

        _logger.info(f"Iteration {iteration} completed: {active_count} active, {stopped_count} stopped")
        return summary

    async def _execute_refine_batch(
        self,
        refiner_tasks: List[RefinerTask],
        iteration: int
    ) -> List[Dict]:
        """
        批量执行refine任务

        Args:
            refiner_tasks: RefinerTask列表
            iteration: 当前迭代轮次

        Returns:
            执行结果列表
        """
        _logger.info(f"Executing {len(refiner_tasks)} refine tasks in parallel")

        # 使用LLM池异步执行
        async def refine_single_task(task: RefinerTask):
            return await self.llm_pool.async_run(
                self._refine_task_sync,
                task,
                iteration
            )

        results = await asyncio.gather(
            *[refine_single_task(task) for task in refiner_tasks],
            return_exceptions=True
        )

        # 处理异常
        processed_results = []
        for i, result in enumerate(results):
            if isinstance(result, Exception):
                _logger.error(f"Task {refiner_tasks[i].task_id} failed: {result}")
                processed_results.append({
                    "task_id": refiner_tasks[i].task_id,
                    "success": False,
                    "error": str(result)
                })
            else:
                processed_results.append(result)

        return processed_results

    def _refine_task_sync(self, llm: LLMAPI, task: RefinerTask, iteration: int) -> Dict:
        """
        同步执行单个refine任务（在线程池中执行）

        Args:
            llm: LLM实例
            task: RefinerTask
            iteration: 当前迭代轮次

        Returns:
            执行结果字典
        """
        try:
            _logger.info(f"Task {task.task_id}: Starting refine (iteration {iteration})")
            refine_start_time = time.time()

            # 读取DSL
            with open(task.dsl_path, 'r', encoding='utf-8') as f:
                dsl_code = f.read()

            # 读取buggy/fixed/root_cause
            with open(task.buggy_path, 'r', encoding='utf-8') as f:
                buggy_code = f.read()
            with open(task.fixed_path, 'r', encoding='utf-8') as f:
                fixed_code = f.read()
            with open(task.root_cause_path, 'r', encoding='utf-8') as f:
                import json
                root_cause_info = json.load(f)
                root_cause = root_cause_info.get("root_cause", "")

            # 提取FP代码
            fp_code = self.fp_selector.extract_fp_code(task.fp_info)

            # 构建RefineInput
            refine_input = RefineInput(
                dsl_code=dsl_code,
                buggy_code=buggy_code,
                fixed_code=fixed_code,
                root_cause=root_cause,
                fp_code=fp_code
            )

            # 执行refine
            refiner = DSLRefiner(llm=llm, input_data=refine_input)
            refined_dsl = refiner.refine()

            refine_elapsed = time.time() - refine_start_time

            # 保存refined DSL
            task.output_dir.mkdir(parents=True, exist_ok=True)
            output_dsl_path = task.output_dir / "dsl.kirin"
            with open(output_dsl_path, 'w', encoding='utf-8') as f:
                f.write(refined_dsl)

            # 保存refine_context.json（会自动计算token使用）
            context_path = task.output_dir / "refine_context.json"
            refiner.serialize_context(context_path)

            # 从context中获取token使用信息
            prompt_tokens = refiner.context.accumulated_prompt_tokens
            completion_tokens = refiner.context.accumulated_completion_tokens
            total_tokens = refiner.context.accumulated_total_tokens

            # 创建并保存TaskInfo
            refine_metrics = RefineMetrics(
                refine_success=True,
                refine_time_seconds=round(refine_elapsed, 2),
                prompt_tokens=prompt_tokens,
                completion_tokens=completion_tokens,
                total_tokens=total_tokens
            )

            # 更新fp_history
            new_fp_history = dict(task.prev_task_info.fp_history)
            new_fp_history[f"iteration_{iteration}"] = task.fp_info

            task_info = TaskInfo(
                task_id=task.task_id,
                iteration=iteration,
                status=TaskStatus.ACTIVE,
                metrics=TaskMetrics(
                    refine_metrics=refine_metrics,
                    detection_metrics=None  # 等待用户检测后更新
                ),
                timestamp=get_current_timestamp(),
                fp_selection=task.fp_selection,
                fp_history=new_fp_history
            )

            self._save_task_info(task_info, task.task_id, iteration)

            _logger.info(f"Task {task.task_id}: Refine succeeded in {refine_elapsed:.2f}s")

            return {
                "task_id": task.task_id,
                "success": True,
                "total_tokens": total_tokens
            }

        except Exception as e:
            _logger.error(f"Task {task.task_id}: Refine failed: {e}")
            import traceback
            traceback.print_exc()

            # 标记为停止
            stopped_task_info = self._create_stopped_task_info(
                task.prev_task_info, iteration, StopReason.REFINE_FAILED, str(e)
            )
            self._save_task_info(stopped_task_info, task.task_id, iteration)

            return {
                "task_id": task.task_id,
                "success": False,
                "error": str(e)
            }

    def _get_detect_result_path(self, task_id: str, iteration: int, scanned_case: str) -> Path:
        """获取检测结果路径"""
        if iteration == 0:
            # Iteration 0使用原始检测结果
            return self.path_mapper.get_ori_detect_result_path(task_id, scanned_case)
        else:
            # Iteration N使用迭代检测结果
            return self.path_mapper.get_iteration_detect_result_path(task_id, iteration, scanned_case)

    def _create_stopped_task_info(
        self,
        prev_task_info: TaskInfo,
        iteration: int,
        stop_reason: StopReason,
        error: Optional[str] = None
    ) -> TaskInfo:
        """创建已停止的TaskInfo"""
        return TaskInfo(
            task_id=prev_task_info.task_id,
            iteration=iteration,
            status=TaskStatus.STOPPED,
            metrics=prev_task_info.metrics,  # 复制上一轮的metrics
            timestamp=get_current_timestamp(),
            fp_selection=None,
            fp_history=prev_task_info.fp_history,
            stop_reason=stop_reason,
            stopped_at=iteration,
            error=error
        )

    def _save_task_info(self, task_info: TaskInfo, task_id: str, iteration: int):
        """保存TaskInfo"""
        task_info_path = self.path_mapper.get_iteration_task_info_path(task_id, iteration)
        task_info.save_to_file(task_info_path)
