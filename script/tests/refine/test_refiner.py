"""
DSL 优化框架测试

使用 pytest 框架重写的测试，支持从中间结果开始调试。
"""
import pytest
from pathlib import Path
from unittest.mock import Mock

# openai 是可选依赖，在需要时导入
try:
    from interface.llm.llm_openai import LLMOpenAI
    OPENAI_AVAILABLE = True
except ImportError:
    OPENAI_AVAILABLE = False

from app.refine.dsl_refiner import DSLRefiner, load_refine_input_from_paths
from app.refine.data_structures import RefineStep
from utils.config import set_config, LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


class TestDSLRefiner:
    """DSL 优化框架测试类"""
    
    @pytest.fixture
    def test_data_paths(self):
        """测试数据路径 fixture"""
        # 可以通过环境变量或配置文件设置测试数据路径
        import os
        base_path = os.getenv("TEST_DATA_BASE", "E:/dataset/Navi")
        
        return {
            'dsl_path': Path(f"{base_path}/final_thesis_datas/ori_dsl/pmd_v1_commits/AvoidInstanceofChecksInCatchClause/3/2.kirin"),
            'buggy_path': Path(f"{base_path}/DEFs/pmd/AvoidInstanceofChecksInCatchClause/3/2/buggy.java"),
            'fixed_path': Path(f"{base_path}/DEFs/pmd/AvoidInstanceofChecksInCatchClause/3/2/fixed.java"),
            'root_cause_path': Path(f"{base_path}/DEFs/pmd/AvoidInstanceofChecksInCatchClause/3/2/info.json"),
            'fp_results_path': Path(f"{base_path}/final_thesis_datas/ori_dsl_detect_results/pmd_v1_commits/AvoidInstanceofChecksInCatchClause/3/2_1_labeled_results.json"),
        }
    
    @pytest.fixture
    def output_dir(self, tmp_path):
        """输出目录 fixture"""
        output = tmp_path / "refine_output"
        output.mkdir(parents=True, exist_ok=True)
        return output
    
    @pytest.fixture
    def llm(self):
        """LLM fixture（可选，用于完整流程测试）"""
        if not OPENAI_AVAILABLE:
            pytest.skip("openai package not installed")
        
        try:
            config = set_config("yunwu")
            model_name = config.get("openai").get("model")
            
            return LLMOpenAI(
                base_url=config.get("openai").get("base_url"),
                api_key=config.get("openai").get("api_keys")[0],
                model_name=model_name
            )
        except Exception as e:
            _logger.warning(f"Failed to initialize LLM: {e}")
            pytest.skip(f"LLM not available for testing: {e}")
    
    @pytest.mark.requires_llm
    @pytest.mark.skipif(
        not Path("E:/dataset/Navi").exists(),
        reason="Test data not available"
    )
    def test_refiner_full(self, test_data_paths, output_dir, llm):
        """
        完整流程：从头开始执行所有步骤（包含 LLM 调用）
        
        注意：这个测试需要 LLM API 可用，并且会消耗 API 调用。
        """
        # 检查测试数据文件是否存在
        for path in test_data_paths.values():
            if not path.exists():
                pytest.skip(f"Test data file not found: {path}")
        
        try:
            # 加载输入数据
            input_data = load_refine_input_from_paths(
                dsl_path=test_data_paths['dsl_path'],
                buggy_path=test_data_paths['buggy_path'],
                fixed_path=test_data_paths['fixed_path'],
                root_cause_path=test_data_paths['root_cause_path'],
                fp_results_path=test_data_paths['fp_results_path']
            )
            
            _logger.info("Input data loaded successfully")
            _logger.info(f"DSL length: {len(input_data.dsl_code)}")
            _logger.info(f"Buggy code length: {len(input_data.buggy_code)}")
            _logger.info(f"Fixed code length: {len(input_data.fixed_code)}")
            
            # 创建优化器
            refiner = DSLRefiner(llm=llm, input_data=input_data)
            
            # 执行优化
            refined_dsl = refiner.refine()
            
            assert refined_dsl is not None, "Refinement should succeed"
            assert len(refined_dsl.strip()) > 0, "Refined DSL should not be empty"
            
            _logger.info("Refinement completed successfully!")
            
            # 保存结果
            refined_dsl_path = output_dir / "refined_dsl.kirin"
            with open(refined_dsl_path, "w", encoding="utf-8") as f:
                f.write(refined_dsl)
            _logger.info(f"Refined DSL saved to {refined_dsl_path}")
            
            # 保存上下文（用于后续调试）
            context_path = output_dir / "refine_context.json"
            refiner.serialize_context(context_path)
            _logger.info(f"Context saved to {context_path} for debugging")
            
        except FileNotFoundError as e:
            pytest.fail(f"File not found: {e}")
        except Exception as e:
            pytest.fail(f"Error during refinement: {e}")
    
    @pytest.mark.skipif(
        not Path("E:/dataset/Navi").exists(),
        reason="Test data not available"
    )
    def test_refiner_from_construct(self, test_data_paths, output_dir):
        """
        从构造 DSL 步骤开始调试（不需要 LLM 调用）
        
        需要先运行 test_refiner_full 生成 context.json 文件。
        """
        # 检查测试数据文件是否存在
        for path in test_data_paths.values():
            if not path.exists():
                pytest.skip(f"Test data file not found: {path}")
        
        # 查找 context 文件（可能在输出目录中）
        context_json_path = output_dir / "refine_context.json"
        if not context_json_path.exists():
            # 尝试从默认位置加载
            default_context = Path("exp/thesis/refine/output/refine_context.json")
            if default_context.exists():
                context_json_path = default_context
            else:
                pytest.skip("Context file not found. Run test_refiner_full first.")
        
        try:
            # 加载输入数据
            input_data = load_refine_input_from_paths(
                dsl_path=test_data_paths['dsl_path'],
                buggy_path=test_data_paths['buggy_path'],
                fixed_path=test_data_paths['fixed_path'],
                root_cause_path=test_data_paths['root_cause_path'],
                fp_results_path=test_data_paths['fp_results_path']
            )
            
            _logger.info("Input data loaded successfully")
            _logger.info(f"Loading context from {context_json_path} and starting from construct step")
            
            # 创建优化器（不需要 LLM）
            refiner = DSLRefiner(llm=None, input_data=input_data)
            
            # 加载上下文
            refiner.load_context_from_json(context_json_path)
            
            # 从构造步骤开始执行
            refiner.start_from_step(RefineStep.CONSTRUCT_DSL, input_data=input_data)
            
            # 执行优化（只执行构造 DSL 步骤）
            refined_dsl = refiner.refine()
            
            assert refined_dsl is not None, "Refinement should succeed"
            assert len(refined_dsl.strip()) > 0, "Refined DSL should not be empty"
            
            _logger.info("Refinement completed successfully!")
            
            # 保存结果
            refined_dsl_path = output_dir / "refined_dsl.kirin"
            with open(refined_dsl_path, "w", encoding="utf-8") as f:
                f.write(refined_dsl)
            _logger.info(f"Refined DSL saved to {refined_dsl_path}")
            
        except FileNotFoundError as e:
            pytest.fail(f"File not found: {e}")
        except Exception as e:
            pytest.fail(f"Error during refinement: {e}")
