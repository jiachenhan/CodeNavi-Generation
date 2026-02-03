"""
路径映射工具模块

提供DSL、DEF、检测结果等文件的路径映射和解析功能。

路径映射规则：
- 任务ID格式：{dataset}/{checker_name}/{group}/{case}
- 示例：pmd_v1_commits/AvoidInstanceofChecksInCatchClause/3/2
"""
from pathlib import Path
from typing import Tuple, Optional, List
from dataclasses import dataclass

from exp.thesis.refine.experiment_config import ExperimentConfig
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


@dataclass
class TaskPaths:
    """
    任务相关的所有路径

    包含单个DSL任务的所有输入和输出路径。
    """
    # 任务标识
    task_id: str                        # 任务ID（相对路径）
    dataset: str                        # 数据集名称（pmd_v1_commits/codeql_v1_commits）
    checker_name: str                   # Checker名称
    group: str                          # 组号
    case: str                           # Case号

    # 输入路径
    ori_dsl_path: Path                  # 原始DSL路径
    buggy_path: Path                    # Buggy代码路径
    fixed_path: Path                    # Fixed代码路径
    info_path: Path                     # info.json路径
    ori_detect_result_path: Path        # 原始检测结果路径

    # 输出路径（单轮迭代）
    def get_iteration_output_dir(self, iteration: int) -> Path:
        """获取指定迭代轮次的输出目录"""
        raise NotImplementedError("Use PathMapper to get iteration paths")


class PathMapper:
    """
    路径映射器

    根据配置和任务ID提供所有相关路径的映射。
    """

    def __init__(self, config: ExperimentConfig):
        """
        初始化路径映射器

        Args:
            config: 实验配置
        """
        self.config = config

    def parse_task_id(self, task_id: str) -> Tuple[str, str, str, str]:
        """
        解析任务ID

        Args:
            task_id: 任务ID（格式：{dataset}/{checker}/{group}/{case}）

        Returns:
            (dataset, checker_name, group, case)

        Raises:
            ValueError: 任务ID格式不正确
        """
        parts = task_id.split('/')
        if len(parts) != 4:
            raise ValueError(
                f"Invalid task_id format: {task_id}. "
                f"Expected format: {{dataset}}/{{checker}}/{{group}}/{{case}}"
            )

        dataset, checker_name, group, case = parts
        return dataset, checker_name, group, case

    def get_tool_name(self, dataset: str) -> str:
        """
        从dataset名称推导tool名称

        Args:
            dataset: 数据集名称（如 pmd_v1_commits）

        Returns:
            工具名称（如 pmd）
        """
        # pmd_v1_commits -> pmd
        # codeql_v1_commits -> codeql
        return dataset.split('_')[0]

    def get_ori_dsl_path(self, task_id: str) -> Path:
        """
        获取原始DSL文件路径

        Args:
            task_id: 任务ID

        Returns:
            原始DSL文件路径
        """
        # E:/dataset/Navi/final_thesis_datas/ori_dsl/{task_id}.kirin
        return self.config.ori_dsl_root / f"{task_id}.kirin"

    def get_def_paths(self, task_id: str) -> Tuple[Path, Path, Path]:
        """
        获取DEF数据路径（buggy, fixed, info.json）

        Args:
            task_id: 任务ID

        Returns:
            (buggy_path, fixed_path, info_path)
        """
        dataset, checker_name, group, case = self.parse_task_id(task_id)
        tool = self.get_tool_name(dataset)
        defs_dataset_name = "ql" if tool == "codeql" else tool

        # E:/dataset/Navi/DEFs/{tool}/{checker}/{group}/{case}/
        def_dir = self.config.defs_root / defs_dataset_name / checker_name / group / case

        buggy_path = def_dir / "buggy.java"
        fixed_path = def_dir / "fixed.java"
        info_path = def_dir / "info.json"

        return buggy_path, fixed_path, info_path

    def get_ori_detect_result_path(self, task_id: str, scanned_case: str) -> Path:
        """
        获取原始检测结果路径

        Args:
            task_id: 任务ID
            scanned_case: 扫描的case编号（必须提供，且应与dsl case不同）
                         - 通常为"2"（case1的DSL检测case2）

        Returns:
            原始检测结果文件路径
        """
        dataset, checker_name, group, dsl_case = self.parse_task_id(task_id)

        # 确保scanned_case与dsl_case不同
        if scanned_case == dsl_case:
            raise ValueError(
                f"scanned_case ({scanned_case}) should be different from dsl_case ({dsl_case}). "
                f"Typically: dsl_case=1, scanned_case=2"
            )

        # E:/dataset/Navi/final_thesis_datas/ori_dsl_detect_results/{dataset}/{checker}/{group}/{dsl_case}_{scanned_case}_labeled_results.json
        result_dir = self.config.ori_detect_results_root / dataset / checker_name / group
        result_file = result_dir / f"{dsl_case}_{scanned_case}_labeled_results.json"

        return result_file

    def get_iteration_output_dir(self, task_id: str, iteration: int) -> Path:
        """
        获取指定迭代轮次的输出目录

        Args:
            task_id: 任务ID
            iteration: 迭代轮次

        Returns:
            输出目录路径
        """
        # E:/dataset/Navi/final_thesis_datas/iterExp/iteration_{N}/{task_id}/
        return self.config.iter_exp_root / f"iteration_{iteration}" / task_id

    def get_iteration_dsl_path(self, task_id: str, iteration: int) -> Path:
        """
        获取指定迭代轮次的DSL文件路径

        Args:
            task_id: 任务ID
            iteration: 迭代轮次

        Returns:
            DSL文件路径
        """
        output_dir = self.get_iteration_output_dir(task_id, iteration)
        return output_dir / "dsl.kirin"

    def get_iteration_task_info_path(self, task_id: str, iteration: int) -> Path:
        """
        获取指定迭代轮次的task_info.json路径

        Args:
            task_id: 任务ID
            iteration: 迭代轮次

        Returns:
            task_info.json路径
        """
        output_dir = self.get_iteration_output_dir(task_id, iteration)
        return output_dir / "task_info.json"

    def get_iteration_refine_context_path(self, task_id: str, iteration: int) -> Path:
        """
        获取指定迭代轮次的refine_context.json路径

        Args:
            task_id: 任务ID
            iteration: 迭代轮次

        Returns:
            refine_context.json路径
        """
        output_dir = self.get_iteration_output_dir(task_id, iteration)
        return output_dir / "refine_context.json"

    def get_iteration_detect_results_dir(self, task_id: str, iteration: int) -> Path:
        """
        获取指定迭代轮次的检测结果目录

        Args:
            task_id: 任务ID
            iteration: 迭代轮次

        Returns:
            检测结果目录路径
        """
        output_dir = self.get_iteration_output_dir(task_id, iteration)
        return output_dir / "detect_results"

    def get_iteration_detect_result_path(
        self,
        task_id: str,
        iteration: int,
        scanned_case: str
    ) -> Path:
        """
        获取指定迭代轮次的检测结果文件路径

        Args:
            task_id: 任务ID
            iteration: 迭代轮次
            scanned_case: 扫描的case编号（必须提供，且应与dsl case不同）
                         - 训练阶段（iteration >= 0）：通常为"2"（case1的DSL检测case2）
                         - 验证阶段（RQ4）：通常为"3"（case1的DSL检测case3）

        Returns:
            检测结果文件路径
        """
        _, _, _, dsl_case = self.parse_task_id(task_id)

        # 确保scanned_case与dsl_case不同
        if scanned_case == dsl_case:
            raise ValueError(
                f"scanned_case ({scanned_case}) should be different from dsl_case ({dsl_case}). "
                f"Typically: dsl_case=1, scanned_case=2 (training) or 3 (validation)"
            )

        detect_results_dir = self.get_iteration_detect_results_dir(task_id, iteration)
        return detect_results_dir / f"{dsl_case}_{scanned_case}_labeled_results.json"

    def get_iteration_summary_path(self, iteration: int) -> Path:
        """
        获取指定迭代轮次的汇总文件路径

        Args:
            iteration: 迭代轮次

        Returns:
            iteration_summary.json路径
        """
        return self.config.iter_exp_root / f"iteration_{iteration}" / "iteration_summary.json"

    def get_task_paths(self, task_id: str, scanned_case: str) -> TaskPaths:
        """
        获取任务的所有路径

        Args:
            task_id: 任务ID
            scanned_case: 扫描的case编号（必须提供，且应与dsl case不同）
                         - 通常为"2"（case1的DSL检测case2）

        Returns:
            TaskPaths对象
        """
        dataset, checker_name, group, case = self.parse_task_id(task_id)

        ori_dsl_path = self.get_ori_dsl_path(task_id)
        buggy_path, fixed_path, info_path = self.get_def_paths(task_id)
        ori_detect_result_path = self.get_ori_detect_result_path(task_id, scanned_case)

        return TaskPaths(
            task_id=task_id,
            dataset=dataset,
            checker_name=checker_name,
            group=group,
            case=case,
            ori_dsl_path=ori_dsl_path,
            buggy_path=buggy_path,
            fixed_path=fixed_path,
            info_path=info_path,
            ori_detect_result_path=ori_detect_result_path
        )

    def scan_dsl_files(self) -> List[str]:
        """
        扫描所有符合条件的DSL文件

        Returns:
            任务ID列表
        """
        from fnmatch import fnmatch

        task_ids = []

        for pattern in self.config.dsl_patterns:
            for dsl_path in self.config.ori_dsl_root.glob(pattern):
                if not dsl_path.is_file():
                    continue

                relative_path = dsl_path.relative_to(self.config.ori_dsl_root)
                task_id = str(relative_path.with_suffix('')).replace('\\', '/')

                excluded = False
                for exclude_pattern in self.config.exclude_patterns:
                    if fnmatch(task_id, exclude_pattern):
                        excluded = True
                        break

                if not excluded:
                    task_ids.append(task_id)

        _logger.info(f"Scanned {len(task_ids)} DSL files")
        return sorted(task_ids)

    def validate_input_paths(self, task_id: str, scanned_case: str) -> bool:
        """
        验证任务的输入路径是否存在

        Args:
            task_id: 任务ID
            scanned_case: 扫描的case编号（默认为"2"）

        Returns:
            所有必需的输入文件是否存在
        """
        try:
            task_paths = self.get_task_paths(task_id, scanned_case)

            missing_paths = []

            if not task_paths.ori_dsl_path.exists():
                missing_paths.append(str(task_paths.ori_dsl_path))

            if not task_paths.buggy_path.exists():
                missing_paths.append(str(task_paths.buggy_path))

            if not task_paths.fixed_path.exists():
                missing_paths.append(str(task_paths.fixed_path))

            if not task_paths.info_path.exists():
                missing_paths.append(str(task_paths.info_path))

            # 原始检测结果可能不存在（取决于是否已经运行过检测）
            # 所以不作为必需路径

            if missing_paths:
                _logger.warning(
                    f"Task {task_id}: Missing paths:\n" +
                    "\n".join(f"  - {p}" for p in missing_paths)
                )
                return False

            return True

        except Exception as e:
            _logger.error(f"Task {task_id}: Error validating paths: {e}")
            return False


# ============================================================================
# 辅助函数
# ============================================================================

def create_task_id(dataset: str, checker: str, group: str, case: str) -> str:
    """
    创建任务ID

    Args:
        dataset: 数据集名称
        checker: Checker名称
        group: 组号
        case: Case号

    Returns:
        任务ID
    """
    return f"{dataset}/{checker}/{group}/{case}"
