"""
数据结构定义 - 全局演化报告

定义用于跨迭代分析的数据结构
"""

from dataclasses import dataclass, field, asdict
from typing import List, Optional, Dict
import json
from pathlib import Path


@dataclass
class GlobalIterationStats:
    """单次迭代的全局统计数据"""
    iteration: int
    active_tasks: int  # 本轮active的任务数

    # 检测指标(所有任务的平均值)
    avg_precision: float
    avg_recall: float
    avg_f1: float
    avg_fp_count: float

    # Refine指标(仅iteration >= 1有效)
    total_tokens: Optional[int] = None
    total_time_seconds: Optional[float] = None

    # 分工具统计(可选,仅当指定tool_filter时有值)
    tool_stats: Optional[Dict[str, Dict[str, float]]] = None  # {"pmd": {...}, "codeql": {...}}

    def to_dict(self):
        """转换为字典"""
        return asdict(self)

    @classmethod
    def from_dict(cls, data: dict):
        """从字典创建"""
        return cls(**data)


@dataclass
class OverallImprovement:
    """整体改进汇总(初始iteration_0 vs 最终iteration_N)"""
    precision_delta: float  # 最终 - 初始
    recall_delta: float
    f1_delta: float
    avg_fp_reduction_rate: float  # FP减少率 = (初始FP - 最终FP) / 初始FP

    def to_dict(self):
        """转换为字典"""
        return asdict(self)

    @classmethod
    def from_dict(cls, data: dict):
        """从字典创建"""
        return cls(**data)


@dataclass
class GlobalEvolutionReport:
    """全局演化报告"""
    total_tasks: int  # 总任务数
    max_iteration: int  # 最大迭代轮次
    iteration_stats: List[GlobalIterationStats] = field(default_factory=list)
    overall_improvement: Optional[OverallImprovement] = None

    # 分工具统计(可选)
    tool_breakdown: Optional[Dict[str, int]] = None  # {"pmd": 30, "codeql": 20}

    def to_dict(self):
        """转换为字典"""
        return {
            "total_tasks": self.total_tasks,
            "max_iteration": self.max_iteration,
            "iteration_stats": [stat.to_dict() for stat in self.iteration_stats],
            "overall_improvement": self.overall_improvement.to_dict() if self.overall_improvement else None
        }

    @classmethod
    def from_dict(cls, data: dict):
        """从字典创建"""
        return cls(
            total_tasks=data["total_tasks"],
            max_iteration=data["max_iteration"],
            iteration_stats=[GlobalIterationStats.from_dict(s) for s in data["iteration_stats"]],
            overall_improvement=OverallImprovement.from_dict(data["overall_improvement"]) if data.get("overall_improvement") else None
        )

    def save_to_file(self, file_path: Path):
        """保存为JSON文件"""
        file_path.parent.mkdir(parents=True, exist_ok=True)
        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump(self.to_dict(), f, indent=2, ensure_ascii=False)

    @classmethod
    def load_from_file(cls, file_path: Path):
        """从JSON文件加载"""
        with open(file_path, 'r', encoding='utf-8') as f:
            data = json.load(f)
        return cls.from_dict(data)
