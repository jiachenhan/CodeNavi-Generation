"""
测试路径映射工具模块
"""
import pytest
from pathlib import Path
import tempfile

from exp.thesis.refine.experiment_config import ExperimentConfig
from exp.thesis.refine.path_utils import PathMapper, TaskPaths, create_task_id


class TestTaskId:
    """测试任务ID相关功能"""

    def test_create_task_id(self):
        """测试创建任务ID"""
        task_id = create_task_id("pmd_v1_commits", "AvoidInstanceof", "3", "2")
        assert task_id == "pmd_v1_commits/AvoidInstanceof/3/2"

    def test_parse_task_id(self):
        """测试解析任务ID"""
        config = ExperimentConfig()
        mapper = PathMapper(config)

        task_id = "pmd_v1_commits/AvoidInstanceof/3/2"
        dataset, checker, group, case = mapper.parse_task_id(task_id)

        assert dataset == "pmd_v1_commits"
        assert checker == "AvoidInstanceof"
        assert group == "3"
        assert case == "2"

    def test_parse_invalid_task_id(self):
        """测试解析无效的任务ID"""
        config = ExperimentConfig()
        mapper = PathMapper(config)

        with pytest.raises(ValueError, match="Invalid task_id format"):
            mapper.parse_task_id("invalid/task")

    def test_get_tool_name(self):
        """测试从dataset获取tool名称"""
        config = ExperimentConfig()
        mapper = PathMapper(config)

        assert mapper.get_tool_name("pmd_v1_commits") == "pmd"
        assert mapper.get_tool_name("codeql_v1_commits") == "codeql"


class TestPathMapper:
    """测试路径映射器"""

    @pytest.fixture
    def temp_config(self):
        """创建临时配置"""
        with tempfile.TemporaryDirectory() as tmpdir:
            base_path = Path(tmpdir)

            config = ExperimentConfig(
                dataset_base=base_path,
                ori_dsl_root=base_path / "ori_dsl",
                ori_detect_results_root=base_path / "ori_detect_results",
                defs_root=base_path / "DEFs",
                iter_exp_root=base_path / "iterExp"
            )

            # 创建基础目录
            config.ori_dsl_root.mkdir(parents=True, exist_ok=True)
            config.ori_detect_results_root.mkdir(parents=True, exist_ok=True)
            config.defs_root.mkdir(parents=True, exist_ok=True)
            config.iter_exp_root.mkdir(parents=True, exist_ok=True)

            yield config

    def test_get_ori_dsl_path(self, temp_config):
        """测试获取原始DSL路径"""
        mapper = PathMapper(temp_config)
        task_id = "pmd_v1_commits/AvoidInstanceof/3/2"

        dsl_path = mapper.get_ori_dsl_path(task_id)

        expected = temp_config.ori_dsl_root / "pmd_v1_commits/AvoidInstanceof/3/2.kirin"
        assert dsl_path == expected

    def test_get_def_paths(self, temp_config):
        """测试获取DEF数据路径"""
        mapper = PathMapper(temp_config)
        task_id = "pmd_v1_commits/AvoidInstanceof/3/2"

        buggy_path, fixed_path, info_path = mapper.get_def_paths(task_id)

        expected_dir = temp_config.defs_root / "pmd/AvoidInstanceof/3/2"
        assert buggy_path == expected_dir / "buggy.java"
        assert fixed_path == expected_dir / "fixed.java"
        assert info_path == expected_dir / "info.json"

    def test_get_ori_detect_result_path(self, temp_config):
        """测试获取原始检测结果路径（case1的DSL检测case2）"""
        mapper = PathMapper(temp_config)
        task_id = "pmd_v1_commits/AvoidInstanceof/3/1"  # dsl_case=1

        result_path = mapper.get_ori_detect_result_path(task_id, scanned_case="2")

        expected_dir = temp_config.ori_detect_results_root / "pmd_v1_commits/AvoidInstanceof/3"
        assert result_path == expected_dir / "1_2_labeled_results.json"

    def test_get_ori_detect_result_path_same_case_raises_error(self, temp_config):
        """测试scanned_case与dsl_case相同时抛出异常"""
        mapper = PathMapper(temp_config)
        task_id = "pmd_v1_commits/AvoidInstanceof/3/1"

        with pytest.raises(ValueError, match="should be different from dsl_case"):
            mapper.get_ori_detect_result_path(task_id, scanned_case="1")

    def test_get_iteration_output_dir(self, temp_config):
        """测试获取迭代输出目录"""
        mapper = PathMapper(temp_config)
        task_id = "pmd_v1_commits/AvoidInstanceof/3/2"

        output_dir_0 = mapper.get_iteration_output_dir(task_id, 0)
        output_dir_1 = mapper.get_iteration_output_dir(task_id, 1)

        expected_0 = temp_config.iter_exp_root / "iteration_0/pmd_v1_commits/AvoidInstanceof/3/2"
        expected_1 = temp_config.iter_exp_root / "iteration_1/pmd_v1_commits/AvoidInstanceof/3/2"

        assert output_dir_0 == expected_0
        assert output_dir_1 == expected_1

    def test_get_iteration_dsl_path(self, temp_config):
        """测试获取迭代DSL路径"""
        mapper = PathMapper(temp_config)
        task_id = "pmd_v1_commits/AvoidInstanceof/3/2"

        dsl_path = mapper.get_iteration_dsl_path(task_id, 1)

        expected = temp_config.iter_exp_root / "iteration_1/pmd_v1_commits/AvoidInstanceof/3/2/dsl.kirin"
        assert dsl_path == expected

    def test_get_iteration_task_info_path(self, temp_config):
        """测试获取迭代task_info路径"""
        mapper = PathMapper(temp_config)
        task_id = "pmd_v1_commits/AvoidInstanceof/3/2"

        task_info_path = mapper.get_iteration_task_info_path(task_id, 0)

        expected = temp_config.iter_exp_root / "iteration_0/pmd_v1_commits/AvoidInstanceof/3/2/task_info.json"
        assert task_info_path == expected

    def test_get_iteration_detect_result_path(self, temp_config):
        """测试获取迭代检测结果路径（case1的DSL检测case2）"""
        mapper = PathMapper(temp_config)
        task_id = "pmd_v1_commits/AvoidInstanceof/3/1"  # dsl_case=1

        result_path = mapper.get_iteration_detect_result_path(task_id, 1, scanned_case="2")

        expected = temp_config.iter_exp_root / "iteration_1/pmd_v1_commits/AvoidInstanceof/3/1/detect_results/1_2_labeled_results.json"
        assert result_path == expected

    def test_get_iteration_detect_result_path_case3_validation(self, temp_config):
        """测试获取case3验证检测结果路径（case1的DSL检测case3）"""
        mapper = PathMapper(temp_config)
        task_id = "pmd_v1_commits/AvoidInstanceof/3/1"  # dsl_case=1

        result_path = mapper.get_iteration_detect_result_path(task_id, 5, scanned_case="3")

        expected = temp_config.iter_exp_root / "iteration_5/pmd_v1_commits/AvoidInstanceof/3/1/detect_results/1_3_labeled_results.json"
        assert result_path == expected

    def test_get_iteration_summary_path(self, temp_config):
        """测试获取迭代汇总路径"""
        mapper = PathMapper(temp_config)

        summary_path = mapper.get_iteration_summary_path(1)

        expected = temp_config.iter_exp_root / "iteration_1/iteration_summary.json"
        assert summary_path == expected

    def test_get_task_paths(self, temp_config):
        """测试获取任务的所有路径"""
        mapper = PathMapper(temp_config)
        task_id = "pmd_v1_commits/AvoidInstanceof/3/1"  # dsl_case=1

        task_paths = mapper.get_task_paths(task_id, scanned_case="2")

        assert task_paths.task_id == task_id
        assert task_paths.dataset == "pmd_v1_commits"
        assert task_paths.checker_name == "AvoidInstanceof"
        assert task_paths.group == "3"
        assert task_paths.case == "1"
        assert task_paths.ori_dsl_path.name == "1.kirin"
        assert task_paths.buggy_path.name == "buggy.java"
        assert "1_2_labeled_results.json" in str(task_paths.ori_detect_result_path)

    def test_scan_dsl_files(self, temp_config):
        """测试扫描DSL文件"""
        # 创建测试DSL文件
        dsl_dir = temp_config.ori_dsl_root / "pmd_v1_commits/TestChecker/1"
        dsl_dir.mkdir(parents=True, exist_ok=True)

        (dsl_dir / "1.kirin").write_text("test dsl")
        (dsl_dir / "2.kirin").write_text("test dsl")

        mapper = PathMapper(temp_config)
        task_ids = mapper.scan_dsl_files()

        assert len(task_ids) == 2
        assert "pmd_v1_commits/TestChecker/1/1" in task_ids
        assert "pmd_v1_commits/TestChecker/1/2" in task_ids

    def test_scan_dsl_files_with_exclusion(self, temp_config):
        """测试扫描DSL文件（带排除模式）"""
        # 创建测试DSL文件
        dsl_dir1 = temp_config.ori_dsl_root / "pmd_v1_commits/Checker1/1"
        dsl_dir2 = temp_config.ori_dsl_root / "pmd_v1_commits/Checker2/1"
        dsl_dir1.mkdir(parents=True, exist_ok=True)
        dsl_dir2.mkdir(parents=True, exist_ok=True)

        (dsl_dir1 / "1.kirin").write_text("test")
        (dsl_dir2 / "1.kirin").write_text("test")

        # 添加排除模式
        temp_config.exclude_patterns = ["**/Checker2/**"]

        mapper = PathMapper(temp_config)
        task_ids = mapper.scan_dsl_files()

        assert "pmd_v1_commits/Checker1/1/1" in task_ids
        assert "pmd_v1_commits/Checker2/1/1" not in task_ids

    def test_validate_input_paths_missing_files(self, temp_config):
        """测试验证输入路径（缺少文件）"""
        mapper = PathMapper(temp_config)
        task_id = "pmd_v1_commits/AvoidInstanceof/3/2"

        # 文件不存在，应该返回False
        result = mapper.validate_input_paths(task_id, scanned_case="2")
        assert result is False

    def test_validate_input_paths_all_exist(self, temp_config):
        """测试验证输入路径（所有文件存在）"""
        mapper = PathMapper(temp_config)
        task_id = "pmd_v1_commits/AvoidInstanceof/3/1"  # dsl_case=1

        # 创建所有必需文件
        task_paths = mapper.get_task_paths(task_id, scanned_case="2")

        task_paths.ori_dsl_path.parent.mkdir(parents=True, exist_ok=True)
        task_paths.ori_dsl_path.write_text("test dsl")

        task_paths.buggy_path.parent.mkdir(parents=True, exist_ok=True)
        task_paths.buggy_path.write_text("buggy code")
        task_paths.fixed_path.write_text("fixed code")
        task_paths.info_path.write_text("{}")

        # 现在应该返回True
        result = mapper.validate_input_paths(task_id, scanned_case="2")
        assert result is True
