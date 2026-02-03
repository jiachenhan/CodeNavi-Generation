"""
FP选择器模块

负责从检测结果中选择FP用于约束优化。
支持sequential（顺序选择）和random（随机选择）两种策略。
"""
import json
import random
from pathlib import Path
from typing import List, Optional, Dict, Any

from exp.thesis.refine.experiment_config import FPSelectionStrategy
from exp.thesis.refine.data_structures import FPInfo, FPSelection
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


class FPSelector:
    """FP选择器"""

    def __init__(self, strategy: FPSelectionStrategy = FPSelectionStrategy.SEQUENTIAL):
        """
        初始化FP选择器

        Args:
            strategy: FP选择策略（sequential或random）
        """
        self.strategy = strategy
        _logger.info(f"Initialized FPSelector with strategy: {strategy.value}")

    def select_fp(
        self,
        detect_result_path: Path,
        used_fp_keys: List[str]
    ) -> FPSelection:
        """
        从检测结果中选择1个FP

        Args:
            detect_result_path: 检测结果文件路径（*_labeled_results.json）
            used_fp_keys: 已使用的FP唯一标识列表（来自fp_history）

        Returns:
            FPSelection对象（包含total_fps, remaining_fps_count, selected_fp）
        """
        if not detect_result_path.exists():
            _logger.warning(f"Detect result file not found: {detect_result_path}")
            return FPSelection(
                total_fps_in_results=0,
                remaining_fps_count=0,
                selected_fp=None
            )

        # 读取检测结果
        with open(detect_result_path, 'r', encoding='utf-8') as f:
            detect_results = json.load(f)

        # 提取所有FP
        all_fps = [
            FPInfo.from_detection_result(result)
            for result in detect_results
            if result.get("label") == "fp"
        ]

        total_fps = len(all_fps)

        if total_fps == 0:
            _logger.info(f"No FPs found in {detect_result_path}")
            return FPSelection(
                total_fps_in_results=0,
                remaining_fps_count=0,
                selected_fp=None
            )

        # 过滤已使用的FP
        remaining_fps = [
            fp for fp in all_fps
            if fp.get_unique_key() not in used_fp_keys
        ]

        remaining_count = len(remaining_fps)

        if remaining_count == 0:
            _logger.info(f"All {total_fps} FPs have been used")
            return FPSelection(
                total_fps_in_results=total_fps,
                remaining_fps_count=0,
                selected_fp=None
            )

        # 选择1个FP
        if self.strategy == FPSelectionStrategy.SEQUENTIAL:
            selected_fp = remaining_fps[0]  # 选择第一个未使用的FP
            _logger.info(f"Selected FP (sequential): {selected_fp.get_unique_key()}")
        else:  # random
            selected_fp = random.choice(remaining_fps)
            _logger.info(f"Selected FP (random): {selected_fp.get_unique_key()}")

        return FPSelection(
            total_fps_in_results=total_fps,
            remaining_fps_count=remaining_count,
            selected_fp=selected_fp
        )

    def extract_fp_code(self, fp_info: FPInfo) -> str:
        """
        提取FP的代码片段（用于构建RefineInput.fp_code）

        Args:
            fp_info: FP信息

        Returns:
            纯净的代码片段（不包含额外注释）
        """
        return fp_info.method_source

    def get_used_fp_keys(self, fp_history: Dict[str, FPInfo]) -> List[str]:
        """
        从fp_history中提取已使用的FP唯一标识列表

        Args:
            fp_history: FP历史记录（key: iteration_N, value: FPInfo）

        Returns:
            已使用的FP唯一标识列表
        """
        return [fp.get_unique_key() for fp in fp_history.values()]
