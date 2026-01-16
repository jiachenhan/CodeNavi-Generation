"""
DSL优化框架的测试文件
支持从中间结果开始调试，避免重复调用LLM
"""
from pathlib import Path

from app.refine.dsl_refiner import DSLRefiner, load_refine_input_from_paths
from app.refine.data_structures import RefineStep
from interface.llm.llm_openai import LLMOpenAI
from utils.config import set_config, LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def test_refiner_full():
    """完整流程：从头开始执行所有步骤（包含LLM调用）"""
    # 输出目录
    output_dir = Path("exp/thesis/refine/output")
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # 根据文档中的测试路径加载数据
    dsl_path = Path("E:/dataset/Navi/final_thesis_datas/ori_dsl/pmd_v1_commits/AvoidInstanceofChecksInCatchClause/3/2.kirin")
    buggy_path = Path("E:/dataset/Navi/DEFs/pmd/AvoidInstanceofChecksInCatchClause/3/2/buggy.java")
    fixed_path = Path("E:/dataset/Navi/DEFs/pmd/AvoidInstanceofChecksInCatchClause/3/2/fixed.java")
    root_cause_path = Path("E:/dataset/Navi/DEFs/pmd/AvoidInstanceofChecksInCatchClause/3/2/info.json")
    fp_results_path = Path("E:/dataset/Navi/final_thesis_datas/ori_dsl_detect_results/pmd_v1_commits/AvoidInstanceofChecksInCatchClause/3/2_1_labeled_results.json")
    
    try:
        # 加载输入数据
        input_data = load_refine_input_from_paths(
            dsl_path=dsl_path,
            buggy_path=buggy_path,
            fixed_path=fixed_path,
            root_cause_path=root_cause_path,
            fp_results_path=fp_results_path
        )
        
        _logger.info("Input data loaded successfully")
        _logger.info(f"DSL length: {len(input_data.dsl_code)}")
        _logger.info(f"Buggy code length: {len(input_data.buggy_code)}")
        _logger.info(f"Fixed code length: {len(input_data.fixed_code)}")
        _logger.info(f"Root cause: {input_data.root_cause[:100]}...")
        _logger.info(f"FP code length: {len(input_data.fp_code)}")
        
        # 加载配置
        config = set_config("yunwu")
        model_name = config.get("openai").get("model")
        
        # 初始化LLM
        llm = LLMOpenAI(
            base_url=config.get("openai").get("base_url"),
            api_key=config.get("openai").get("api_keys")[0],
            model_name=model_name
        )
        
        # 创建优化器
        refiner = DSLRefiner(llm=llm, input_data=input_data)
        
        # 执行优化
        refined_dsl = refiner.refine()
        
        if refined_dsl:
            _logger.info("Refinement completed successfully!")
            print("\n" + "="*80)
            print("REFINED DSL:")
            print("="*80)
            print(refined_dsl)
            print("="*80 + "\n")
            
            # 保存结果
            refined_dsl_path = output_dir / "refined_dsl.kirin"
            with open(refined_dsl_path, "w", encoding="utf-8") as f:
                f.write(refined_dsl)
            _logger.info(f"Refined DSL saved to {refined_dsl_path}")
            
            # 保存上下文（用于后续调试）
            context_path = output_dir / "refine_context.json"
            refiner.serialize_context(context_path)
            _logger.info(f"Context saved to {context_path} for debugging")
            
            return True
        else:
            _logger.error("Refinement failed!")
            return False
        
    except FileNotFoundError as e:
        _logger.error(f"File not found: {e}")
        return False
    except Exception as e:
        _logger.error(f"Error during refinement: {e}", exc_info=True)
        return False


def test_refiner_from_construct(context_json_path: Path):
    """从构造DSL步骤开始调试（不需要LLM调用）"""
    # 输出目录
    output_dir = Path("exp/thesis/refine/output")
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # 根据文档中的测试路径加载数据
    dsl_path = Path("E:/dataset/Navi/final_thesis_datas/ori_dsl/pmd_v1_commits/AvoidInstanceofChecksInCatchClause/3/2.kirin")
    buggy_path = Path("E:/dataset/Navi/DEFs/pmd/AvoidInstanceofChecksInCatchClause/3/2/buggy.java")
    fixed_path = Path("E:/dataset/Navi/DEFs/pmd/AvoidInstanceofChecksInCatchClause/3/2/fixed.java")
    root_cause_path = Path("E:/dataset/Navi/DEFs/pmd/AvoidInstanceofChecksInCatchClause/3/2/info.json")
    fp_results_path = Path("E:/dataset/Navi/final_thesis_datas/ori_dsl_detect_results/pmd_v1_commits/AvoidInstanceofChecksInCatchClause/3/2_1_labeled_results.json")
    
    try:
        # 加载输入数据
        input_data = load_refine_input_from_paths(
            dsl_path=dsl_path,
            buggy_path=buggy_path,
            fixed_path=fixed_path,
            root_cause_path=root_cause_path,
            fp_results_path=fp_results_path
        )
        
        _logger.info("Input data loaded successfully")
        _logger.info(f"Loading context from {context_json_path} and starting from construct step")
        
        # 创建优化器（不需要LLM）
        refiner = DSLRefiner(llm=None, input_data=input_data)
        
        # 加载上下文
        refiner.load_context_from_json(context_json_path)
        
        # 从构造步骤开始执行
        refiner.start_from_step(RefineStep.CONSTRUCT_DSL, input_data=input_data)
        
        # 执行优化（只执行构造DSL步骤）
        refined_dsl = refiner.refine()
        
        if refined_dsl:
            _logger.info("Refinement completed successfully!")
            print("\n" + "="*80)
            print("REFINED DSL:")
            print("="*80)
            print(refined_dsl)
            print("="*80 + "\n")
            
            # 保存结果
            refined_dsl_path = output_dir / "refined_dsl.kirin"
            with open(refined_dsl_path, "w", encoding="utf-8") as f:
                f.write(refined_dsl)
            _logger.info(f"Refined DSL saved to {refined_dsl_path}")
            
            return True
        else:
            _logger.error("Failed to construct refined DSL")
            return False
        
    except FileNotFoundError as e:
        _logger.error(f"File not found: {e}")
        return False
    except Exception as e:
        _logger.error(f"Error during refinement: {e}", exc_info=True)
        return False


if __name__ == "__main__":
    # 完整流程
    test_refiner_full()
    
    # 从构造步骤开始调试
    # context_path = Path("exp/thesis/refine/output/refine_context.json")
    # test_refiner_from_construct(context_path)
