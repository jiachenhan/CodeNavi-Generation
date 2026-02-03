"""
演化分析器 - 加载和聚合跨迭代数据

从迭代实验结果中提取数据并进行统计分析
"""

from pathlib import Path
from typing import List, Optional
import json

from exp.thesis.refine.path_utils import PathMapper
from exp.thesis.refine.data_structures import TaskInfo, TaskStatus
from exp.thesis.refine.analysis.data_structures import (
    GlobalEvolutionReport,
    GlobalIterationStats,
    OverallImprovement
)
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


class EvolutionAnalyzer:
    """演化分析器 - 分析跨迭代的实验数据"""

    def __init__(self, path_mapper: PathMapper):
        """
        初始化分析器

        Args:
            path_mapper: 路径映射器
        """
        self.path_mapper = path_mapper

    def _load_task_info(self, task_id: str, iteration: int) -> Optional[TaskInfo]:
        """
        加载指定任务和迭代的task_info.json

        Args:
            task_id: 任务ID
            iteration: 迭代轮次

        Returns:
            TaskInfo对象,如果文件不存在则返回None
        """
        task_info_path = self.path_mapper.get_iteration_task_info_path(task_id, iteration)

        if not task_info_path.exists():
            return None

        try:
            with open(task_info_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            return TaskInfo.from_dict(data)
        except Exception as e:
            _logger.warning(f"Failed to load task_info for {task_id} iteration {iteration}: {e}")
            return None

    def _get_all_task_ids(self, max_iteration: int = 0) -> List[str]:
        """
        获取所有任务ID(递归扫描所有迭代轮次)

        Args:
            max_iteration: 最大迭代轮次,会扫描iteration_0到iteration_N的所有任务

        Returns:
            任务ID列表(去重后的,保留完整层级路径)
        """
        all_task_ids = set()

        # 扫描所有迭代轮次,收集所有任务ID
        for iteration in range(max_iteration + 1):
            iteration_dir = self.path_mapper.config.iter_exp_root / f"iteration_{iteration}"

            if not iteration_dir.exists():
                _logger.warning(f"Iteration directory does not exist: {iteration_dir}")
                continue

            # 递归查找所有task_info.json文件
            for task_info_path in iteration_dir.rglob("task_info.json"):
                # 提取task_id: 从iteration_dir到task_info.json父目录的相对路径
                case_dir = task_info_path.parent
                relative_path = case_dir.relative_to(iteration_dir)
                task_id = str(relative_path).replace('\\', '/')
                all_task_ids.add(task_id)

        task_ids = sorted(all_task_ids)
        _logger.info(f"Found {len(task_ids)} unique tasks across all iterations (iteration_0 to iteration_{max_iteration})")
        return task_ids

    def analyze_global_evolution(
        self,
        max_iteration: int,
        enable_tool_breakdown: bool = True
    ) -> GlobalEvolutionReport:
        """
        分析全局跨迭代演化

        Args:
            max_iteration: 最大迭代轮次
            enable_tool_breakdown: 是否启用分工具统计(默认True)

        Returns:
            GlobalEvolutionReport对象
        """
        _logger.info(f"Starting global evolution analysis (max_iteration={max_iteration})")

        # 从所有迭代中收集任务ID(包括stopped的任务)
        all_task_ids = self._get_all_task_ids(max_iteration=max_iteration)
        total_tasks = len(all_task_ids)

        if total_tasks == 0:
            _logger.error("No tasks found")
            return GlobalEvolutionReport(
                total_tasks=0,
                max_iteration=max_iteration,
                iteration_stats=[],
                overall_improvement=None
            )

        # 统计工具分布
        tool_breakdown = None
        if enable_tool_breakdown:
            tool_breakdown = self._get_tool_breakdown(all_task_ids)
            _logger.info(f"Tool breakdown: {tool_breakdown}")

        iteration_stats_list = []

        # 逐轮迭代统计
        for iteration in range(max_iteration + 1):
            _logger.info(f"Analyzing iteration {iteration}...")
            stats = self._analyze_iteration(
                all_task_ids,
                iteration,
                enable_tool_breakdown=enable_tool_breakdown
            )
            if stats:
                iteration_stats_list.append(stats)

        # 计算整体改进(iteration_0 vs iteration_N)
        overall_improvement = None
        if len(iteration_stats_list) >= 2:
            initial_stats = iteration_stats_list[0]
            final_stats = iteration_stats_list[-1]

            precision_delta = final_stats.avg_precision - initial_stats.avg_precision
            recall_delta = final_stats.avg_recall - initial_stats.avg_recall
            f1_delta = final_stats.avg_f1 - initial_stats.avg_f1

            # FP减少率 = (初始FP - 最终FP) / 初始FP
            fp_reduction_rate = 0.0
            if initial_stats.avg_fp_count > 0:
                fp_reduction_rate = (initial_stats.avg_fp_count - final_stats.avg_fp_count) / initial_stats.avg_fp_count

            overall_improvement = OverallImprovement(
                precision_delta=round(precision_delta, 4),
                recall_delta=round(recall_delta, 4),
                f1_delta=round(f1_delta, 4),
                avg_fp_reduction_rate=round(fp_reduction_rate, 4)
            )

        report = GlobalEvolutionReport(
            total_tasks=total_tasks,
            max_iteration=max_iteration,
            iteration_stats=iteration_stats_list,
            overall_improvement=overall_improvement,
            tool_breakdown=tool_breakdown
        )

        _logger.info("Global evolution analysis completed")
        return report

    def _get_tool_breakdown(self, task_ids: List[str]) -> dict:
        """
        统计任务的工具分布

        Args:
            task_ids: 任务ID列表

        Returns:
            工具分布字典 {"pmd": count, "codeql": count}
        """
        tool_counts = {"pmd": 0, "codeql": 0}

        for task_id in task_ids:
            # task_id格式: {dataset}/{checker}/{group}/{case}
            # dataset格式: pmd_v1_commits 或 codeql_v1_commits
            dataset = task_id.split('/')[0]
            tool = self.path_mapper.get_tool_name(dataset)

            if tool in tool_counts:
                tool_counts[tool] += 1
            else:
                _logger.warning(f"Unknown tool for task {task_id}: {tool}")

        return tool_counts

    def _analyze_iteration(
        self,
        task_ids: List[str],
        iteration: int,
        enable_tool_breakdown: bool = False
    ) -> Optional[GlobalIterationStats]:
        """
        分析单次迭代的全局统计

        对于每个任务,使用它在该轮或之前最近一次的检测指标
        (stopped任务会复用最后一轮的指标)

        Args:
            task_ids: 任务ID列表(固定的任务池)
            iteration: 迭代轮次
            enable_tool_breakdown: 是否启用分工具统计

        Returns:
            GlobalIterationStats对象,如果无有效数据则返回None
        """
        # 汇总TP/FP/FN总数(而不是平均precision/recall/f1)
        total_tp = 0
        total_fp = 0
        total_fn = 0
        active_count = 0
        total_tokens = 0
        total_time_seconds = 0.0

        # 分工具统计(汇总TP/FP/FN)
        tool_metrics = {
            "pmd": {"tp": 0, "fp": 0, "fn": 0},
            "codeql": {"tp": 0, "fp": 0, "fn": 0}
        }

        # 预先计算每个工具的固定任务数(用于后续统计)
        tool_task_counts = {"pmd": 0, "codeql": 0}
        if enable_tool_breakdown:
            for task_id in task_ids:
                dataset = task_id.split('/')[0]
                tool = self.path_mapper.get_tool_name(dataset)
                if tool in tool_task_counts:
                    tool_task_counts[tool] += 1

        for task_id in task_ids:
            # 尝试加载该任务在本轮或之前最近一轮的数据
            task_info = self._load_task_info_with_fallback(task_id, iteration)
            if not task_info:
                continue

            # 统计本轮active任务(只有本轮存在且status=ACTIVE才算)
            current_task_info = self._load_task_info(task_id, iteration)
            if current_task_info and current_task_info.status == TaskStatus.ACTIVE:
                active_count += 1

            # 提取检测指标(使用fallback后的数据)
            detection_metrics = task_info.metrics.detection_metrics
            if detection_metrics:
                # 汇总TP/FP/FN总数
                total_tp += detection_metrics.tp_count
                total_fp += detection_metrics.fp_count
                total_fn += detection_metrics.fn_count

                # 分工具统计
                if enable_tool_breakdown:
                    dataset = task_id.split('/')[0]
                    tool = self.path_mapper.get_tool_name(dataset)
                    if tool in tool_metrics:
                        tool_metrics[tool]["tp"] += detection_metrics.tp_count
                        tool_metrics[tool]["fp"] += detection_metrics.fp_count
                        tool_metrics[tool]["fn"] += detection_metrics.fn_count

            # 提取refine指标(仅当本轮存在且iteration >= 1时有效)
            if iteration >= 1 and current_task_info:
                refine_metrics = current_task_info.metrics.refine_metrics
                if refine_metrics:
                    total_tokens += refine_metrics.total_tokens
                    total_time_seconds += refine_metrics.refine_time_seconds

        # 从汇总的TP/FP/FN计算全局precision/recall/f1
        if total_tp + total_fp == 0:
            _logger.warning(f"No valid metrics for iteration {iteration}")
            return None

        global_precision = total_tp / (total_tp + total_fp) if (total_tp + total_fp) > 0 else 0.0
        global_recall = total_tp / (total_tp + total_fn) if (total_tp + total_fn) > 0 else 0.0
        global_f1 = (2 * global_precision * global_recall / (global_precision + global_recall)
                     if (global_precision + global_recall) > 0 else 0.0)
        avg_fp_count = total_fp / len(task_ids) if len(task_ids) > 0 else 0.0

        stats = GlobalIterationStats(
            iteration=iteration,
            active_tasks=active_count,
            avg_precision=round(global_precision, 4),
            avg_recall=round(global_recall, 4),
            avg_f1=round(global_f1, 4),
            avg_fp_count=round(avg_fp_count, 2)
        )

        # 添加refine指标(仅iteration >= 1)
        if iteration >= 1:
            stats.total_tokens = total_tokens
            stats.total_time_seconds = round(total_time_seconds, 2)

        # 添加分工具统计
        if enable_tool_breakdown:
            tool_stats = {}
            for tool, counts in tool_metrics.items():
                if counts["tp"] + counts["fp"] > 0:  # 如果该工具有数据
                    tool_tp = counts["tp"]
                    tool_fp = counts["fp"]
                    tool_fn = counts["fn"]

                    tool_precision = tool_tp / (tool_tp + tool_fp) if (tool_tp + tool_fp) > 0 else 0.0
                    tool_recall = tool_tp / (tool_tp + tool_fn) if (tool_tp + tool_fn) > 0 else 0.0
                    tool_f1 = (2 * tool_precision * tool_recall / (tool_precision + tool_recall)
                               if (tool_precision + tool_recall) > 0 else 0.0)
                    tool_avg_fp = tool_fp / tool_task_counts[tool] if tool_task_counts[tool] > 0 else 0.0

                    tool_stats[tool] = {
                        "count": tool_task_counts[tool],  # 使用固定的任务总数
                        "avg_precision": round(tool_precision, 4),
                        "avg_recall": round(tool_recall, 4),
                        "avg_f1": round(tool_f1, 4),
                        "avg_fp_count": round(tool_avg_fp, 2)
                    }
            stats.tool_stats = tool_stats

        return stats

    def _load_task_info_with_fallback(
        self,
        task_id: str,
        iteration: int
    ) -> Optional[TaskInfo]:
        """
        加载任务信息,如果本轮不存在则回退到之前最近的一轮

        Args:
            task_id: 任务ID
            iteration: 当前迭代轮次

        Returns:
            TaskInfo对象,如果所有迭代都不存在则返回None
        """
        # 从当前迭代往前找,直到找到第一个存在的task_info
        for i in range(iteration, -1, -1):
            task_info = self._load_task_info(task_id, i)
            if task_info:
                return task_info

        return None
