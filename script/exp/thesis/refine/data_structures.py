"""
实验数据结构定义

定义DSL迭代优化实验中使用的所有数据结构，包括任务信息、FP信息、指标等。
"""
from dataclasses import dataclass, field, asdict
from typing import Optional, Dict, List, Any
from pathlib import Path
from enum import Enum
from datetime import datetime
import json


# ============================================================================
# 枚举类型
# ============================================================================

class TaskStatus(Enum):
    """任务状态"""
    ACTIVE = "active"      # 活跃状态，可以继续迭代
    STOPPED = "stopped"    # 停止状态，不再继续迭代


class StopReason(Enum):
    """停止原因"""
    NO_FP = "no_fp"                          # Iteration 0时无FP
    NO_FP_CONVERGED = "no_fp_converged"      # Iteration N后FP数量降为0
    ALL_FPS_EXHAUSTED = "all_fps_exhausted"  # 所有FP都已使用过
    REFINE_FAILED = "refine_failed"          # Refine执行失败
    METRICS_DEGRADED = "metrics_degraded"    # 指标退化（F1下降）


# ============================================================================
# FP信息数据结构
# ============================================================================

@dataclass
class FPInfo:
    """
    FP (False Positive) 信息

    包含单个FP的完整信息，从检测结果中提取。
    """
    label: str                          # 标签（应该是"fp"）
    file_name: str                      # 文件名
    function_name: str                  # 函数名
    begin_line: int                     # 起始行号
    end_line: int                       # 结束行号
    report_line: int                    # 报告行号
    method_signature: str               # 方法签名
    method_source: str                  # 代码片段（对应JSON中的method_source字段）

    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return asdict(self)

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "FPInfo":
        """从字典创建"""
        return cls(**data)

    @classmethod
    def from_detection_result(cls, result: Dict[str, Any]) -> "FPInfo":
        """
        从检测结果条目创建FPInfo

        Args:
            result: 检测结果中的单个条目（来自labeled_results.json）

        Returns:
            FPInfo对象
        """
        navi_warning = result.get("navi_warning", {})

        return cls(
            label=result.get("label", "fp"),
            file_name=result.get("file_name", ""),
            function_name=navi_warning.get("function_name", "") if navi_warning else "",
            begin_line=result.get("begin_line", 0),
            end_line=result.get("end_line", 0),
            report_line=navi_warning.get("report_line", 0) if navi_warning else 0,
            method_signature=result.get("method_signature", ""),
            method_source=result.get("method_source", "")
        )

    def get_unique_key(self) -> str:
        """
        获取FP的唯一标识

        用于判断两个FP是否相同（用于过滤已使用的FP）
        """
        return f"{self.file_name}:{self.function_name}:{self.report_line}"


# ============================================================================
# FP选择信息
# ============================================================================

@dataclass
class FPSelection:
    """
    FP选择信息

    记录单轮迭代中的FP选择详情。
    """
    total_fps_in_results: int           # 检测结果中的总FP数量
    remaining_fps_count: int            # 剩余未使用的FP数量
    selected_fp: Optional[FPInfo]       # 本轮选择的FP

    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            "total_fps_in_results": self.total_fps_in_results,
            "remaining_fps_count": self.remaining_fps_count,
            "selected_fp": self.selected_fp.to_dict() if self.selected_fp else None
        }

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "FPSelection":
        """从字典创建"""
        selected_fp_data = data.get("selected_fp")
        selected_fp = FPInfo.from_dict(selected_fp_data) if selected_fp_data else None

        return cls(
            total_fps_in_results=data["total_fps_in_results"],
            remaining_fps_count=data["remaining_fps_count"],
            selected_fp=selected_fp
        )


# ============================================================================
# 检测指标
# ============================================================================

@dataclass
class DetectionMetrics:
    """
    检测指标

    包含检测结果的统计信息和评估指标。
    """
    total_warnings: int                 # 总警告数
    tp_count: int                       # True Positive数量
    fp_count: int                       # False Positive数量
    fn_count: int                       # False Negative数量
    precision: float                    # 精确率
    recall: float                       # 召回率
    f1_score: float                     # F1分数

    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return asdict(self)

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "DetectionMetrics":
        """从字典创建"""
        return cls(**data)

    @classmethod
    def from_counts(cls, tp: int, fp: int, fn: int) -> "DetectionMetrics":
        """
        从TP/FP/FN计数创建检测指标

        Args:
            tp: True Positive数量
            fp: False Positive数量
            fn: False Negative数量

        Returns:
            DetectionMetrics对象
        """
        total_warnings = tp + fp
        precision = tp / total_warnings if total_warnings > 0 else 0.0
        recall = tp / (tp + fn) if (tp + fn) > 0 else 0.0
        f1_score = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0.0

        return cls(
            total_warnings=total_warnings,
            tp_count=tp,
            fp_count=fp,
            fn_count=fn,
            precision=round(precision, 4),
            recall=round(recall, 4),
            f1_score=round(f1_score, 4)
        )


# ============================================================================
# Refine指标
# ============================================================================

@dataclass
class RefineMetrics:
    """
    Refine执行指标

    记录单次refine执行的性能信息。
    """
    refine_success: bool                # Refine是否成功
    refine_time_seconds: float          # Refine耗时（秒）
    prompt_tokens: int                  # Prompt tokens数量
    completion_tokens: int              # Completion tokens数量
    total_tokens: int                   # 总tokens数量

    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return asdict(self)

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "RefineMetrics":
        """从字典创建"""
        return cls(**data)


# ============================================================================
# 任务指标（整合所有指标）
# ============================================================================

@dataclass
class TaskMetrics:
    """
    任务指标

    整合refine指标和检测指标。
    """
    refine_metrics: Optional[RefineMetrics]     # Refine指标（iteration 0无）
    detection_metrics: Optional[DetectionMetrics]  # 检测指标

    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        result = {}

        if self.refine_metrics:
            result.update(self.refine_metrics.to_dict())

        if self.detection_metrics:
            result["detection_metrics"] = self.detection_metrics.to_dict()

        return result

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "TaskMetrics":
        """从字典创建"""
        # 检测是否包含refine指标
        refine_metrics = None
        if "refine_success" in data:
            refine_metrics = RefineMetrics(
                refine_success=data["refine_success"],
                refine_time_seconds=data.get("refine_time_seconds", 0.0),
                prompt_tokens=data.get("prompt_tokens", 0),
                completion_tokens=data.get("completion_tokens", 0),
                total_tokens=data.get("total_tokens", 0)
            )

        # 检测指标
        detection_metrics = None
        if "detection_metrics" in data:
            detection_metrics = DetectionMetrics.from_dict(data["detection_metrics"])

        return cls(
            refine_metrics=refine_metrics,
            detection_metrics=detection_metrics
        )


# ============================================================================
# 任务信息 (task_info.json的完整结构)
# ============================================================================

@dataclass
class TaskInfo:
    """
    任务信息

    对应task_info.json的完整结构。
    区分Iteration 0和Iteration N (N >= 1)。
    """
    task_id: str                                # 任务ID
    iteration: int                              # 迭代轮次
    status: TaskStatus                          # 任务状态
    metrics: TaskMetrics                        # 指标
    timestamp: str                              # 时间戳

    # 以下字段仅Iteration N (N >= 1)有
    fp_selection: Optional[FPSelection] = None  # FP选择信息
    fp_history: Dict[str, FPInfo] = field(default_factory=dict)  # FP历史（key: iteration_N）

    # 停止相关字段
    stop_reason: Optional[StopReason] = None    # 停止原因
    stopped_at: Optional[int] = None            # 停止时的迭代轮次
    error: Optional[str] = None                 # 错误信息（refine失败时）

    def to_dict(self) -> Dict[str, Any]:
        """转换为字典（用于JSON序列化）"""
        result = {
            "task_id": self.task_id,
            "iteration": self.iteration,
            "status": self.status.value,
            "metrics": self.metrics.to_dict(),
            "timestamp": self.timestamp
        }

        # 添加可选字段
        if self.fp_selection:
            result["fp_selection"] = self.fp_selection.to_dict()

        if self.fp_history:
            result["fp_history"] = {
                key: fp.to_dict() for key, fp in self.fp_history.items()
            }

        if self.stop_reason:
            result["stop_reason"] = self.stop_reason.value

        if self.stopped_at is not None:
            result["stopped_at"] = self.stopped_at

        if self.error:
            result["error"] = self.error

        return result

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "TaskInfo":
        """从字典创建"""
        # 解析枚举
        status = TaskStatus(data["status"])
        stop_reason = StopReason(data["stop_reason"]) if "stop_reason" in data else None

        # 解析指标
        metrics = TaskMetrics.from_dict(data["metrics"])

        # 解析FP选择
        fp_selection = None
        if "fp_selection" in data and data["fp_selection"]:
            fp_selection = FPSelection.from_dict(data["fp_selection"])

        # 解析FP历史
        fp_history = {}
        if "fp_history" in data:
            for key, fp_data in data["fp_history"].items():
                fp_history[key] = FPInfo.from_dict(fp_data)

        return cls(
            task_id=data["task_id"],
            iteration=data["iteration"],
            status=status,
            metrics=metrics,
            timestamp=data["timestamp"],
            fp_selection=fp_selection,
            fp_history=fp_history,
            stop_reason=stop_reason,
            stopped_at=data.get("stopped_at"),
            error=data.get("error")
        )

    def save_to_file(self, file_path: Path):
        """保存到JSON文件"""
        file_path.parent.mkdir(parents=True, exist_ok=True)
        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump(self.to_dict(), f, indent=2, ensure_ascii=False)

    @classmethod
    def load_from_file(cls, file_path: Path) -> "TaskInfo":
        """从JSON文件加载"""
        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        return cls.from_dict(data)


# ============================================================================
# Refiner任务（用于批量执行）
# ============================================================================

@dataclass
class RefinerTask:
    """
    Refiner任务

    用于批量执行refine时的任务描述。
    """
    task_id: str                        # 任务ID
    iteration: int                      # 迭代轮次
    dsl_path: Path                      # DSL文件路径（上一轮或原始）
    output_dir: Path                    # 输出目录
    status: TaskStatus = TaskStatus.ACTIVE

    # 输入数据路径（用于构建RefineInput）
    buggy_path: Optional[Path] = None
    fixed_path: Optional[Path] = None
    root_cause_path: Optional[Path] = None
    fp_info: Optional[FPInfo] = None    # 选择的FP信息

    # 执行结果
    refined_dsl: Optional[str] = None
    error: Optional[str] = None


# ============================================================================
# 迭代汇总信息
# ============================================================================

@dataclass
class IterationSummary:
    """
    迭代汇总信息

    记录单轮迭代的整体统计信息。
    """
    iteration: int                      # 迭代轮次
    total_tasks: int                    # 总任务数
    active_tasks: int                   # 活跃任务数
    stopped_tasks: int                  # 停止任务数
    refine_success_count: int           # Refine成功数量
    refine_failed_count: int            # Refine失败数量
    total_time_seconds: float           # 总耗时
    total_tokens: int                   # 总tokens
    stop_reasons: Dict[str, int]        # 停止原因统计

    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return asdict(self)

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "IterationSummary":
        """从字典创建"""
        return cls(**data)

    def save_to_file(self, file_path: Path):
        """保存到JSON文件"""
        file_path.parent.mkdir(parents=True, exist_ok=True)
        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump(self.to_dict(), f, indent=2, ensure_ascii=False)

    @classmethod
    def load_from_file(cls, file_path: Path) -> "IterationSummary":
        """从JSON文件加载"""
        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        return cls.from_dict(data)


# ============================================================================
# 辅助函数
# ============================================================================

def get_current_timestamp() -> str:
    """获取当前时间戳（格式化字符串）"""
    return datetime.now().strftime("%Y-%m-%d %H:%M:%S")
