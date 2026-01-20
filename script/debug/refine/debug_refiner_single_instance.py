"""
DSL 优化框架调试脚本 - 单实例调试

⚠️ 注意：这是一个调试脚本，不是单元测试。
用于调试特定实例（AvoidInstanceofChecksInCatchClause/3/2）的 refine 流程。

使用方法：
    # 运行完整流程（包含 LLM 调用）
    python debug/refine/debug_refiner_single_instance.py --full
    
    # 从构造 DSL 步骤开始（不需要 LLM，需要先运行 --full 生成 context）
    python debug/refine/debug_refiner_single_instance.py --from-construct
    
    # 或者直接运行（默认使用 --full）
    python debug/refine/debug_refiner_single_instance.py
"""
import sys
import logging
import argparse
from pathlib import Path

# 添加项目根目录到路径
project_root = Path(__file__).parent.parent.parent
if str(project_root) not in sys.path:
    sys.path.insert(0, str(project_root))

# 导入项目模块
try:
    from interface.llm.llm_openai import LLMOpenAI
    OPENAI_AVAILABLE = True
except ImportError:
    OPENAI_AVAILABLE = False

from app.refine.dsl_refiner import DSLRefiner, load_refine_input_from_paths
from app.refine.data_structures import RefineStep
from utils.config import set_config, LoggerConfig
from tests.conftest import TestDataPaths  # 复用测试路径配置

# 获取 logger
_logger = LoggerConfig.get_logger(__name__)


class RefinerDebugger:
    """
    DSL Refiner 调试器
    
    用于调试特定实例的 refine 流程。
    """
    
    def __init__(self):
        """初始化调试器"""
        # 使用 TestDataPaths 获取路径配置
        self.test_data_paths = TestDataPaths()
        
        # 输出目录（在脚本所在目录下）
        self.output_dir = Path(__file__).parent / "refine_output"
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
        # 定义测试用例路径
        self.test_data = {
            'dsl_path': self.test_data_paths.dsl_root_path / "pmd_v1_commits" / "AvoidInstanceofChecksInCatchClause" / "3" / "2.kirin",
            'buggy_path': self.test_data_paths.defs_root_path / "pmd" / "AvoidInstanceofChecksInCatchClause" / "3" / "2" / "buggy.java",
            'fixed_path': self.test_data_paths.defs_root_path / "pmd" / "AvoidInstanceofChecksInCatchClause" / "3" / "2" / "fixed.java",
            'root_cause_path': self.test_data_paths.defs_root_path / "pmd" / "AvoidInstanceofChecksInCatchClause" / "3" / "2" / "info.json",
            'fp_results_path': self.test_data_paths.detect_results_root_path / "pmd_v1_commits" / "AvoidInstanceofChecksInCatchClause" / "3" / "2_1_labeled_results.json",
        }
        
        # 输出文件路径
        self.refined_dsl_path = self.output_dir / "refined_dsl.kirin"
        self.context_path = self.output_dir / "refine_context.json"
    
    def check_test_data_files(self) -> bool:
        """检查测试数据文件是否存在"""
        missing_files = []
        for key, path in self.test_data.items():
            if not path.exists():
                missing_files.append(f"{key}: {path}")
        
        if missing_files:
            _logger.error("Missing test data files:")
            for file in missing_files:
                _logger.error(f"  - {file}")
            return False
        return True
    
    def create_llm(self):
        """创建 LLM 实例"""
        if not OPENAI_AVAILABLE:
            raise RuntimeError("openai package not installed")
        
        try:
            config = set_config("yunwu")
            model_name = config.get("openai").get("model")
            
            return LLMOpenAI(
                base_url=config.get("openai").get("base_url"),
                api_key=config.get("openai").get("api_keys")[0],
                model_name=model_name
            )
        except Exception as e:
            _logger.error(f"Failed to initialize LLM: {e}")
            raise
    
    def run_full_refinement(self):
        """
        完整流程：从头开始执行所有步骤（包含 LLM 调用）
        
        注意：这个流程需要 LLM API 可用，并且会消耗 API 调用。
        """
        _logger.info("=" * 60)
        _logger.info("Running full refinement process (with LLM)")
        _logger.info("=" * 60)
        
        # 检查测试数据文件
        if not self.check_test_data_files():
            raise FileNotFoundError("Test data files not found")
        
        # 加载输入数据
        _logger.info("Loading input data...")
        input_data = load_refine_input_from_paths(
            dsl_path=self.test_data['dsl_path'],
            buggy_path=self.test_data['buggy_path'],
            fixed_path=self.test_data['fixed_path'],
            root_cause_path=self.test_data['root_cause_path'],
            fp_results_path=self.test_data['fp_results_path']
        )
        
        _logger.info("Input data loaded successfully")
        _logger.info(f"DSL length: {len(input_data.dsl_code)}")
        _logger.info(f"Buggy code length: {len(input_data.buggy_code)}")
        _logger.info(f"Fixed code length: {len(input_data.fixed_code)}")
        
        # 创建 LLM 实例
        _logger.info("Initializing LLM...")
        llm = self.create_llm()
        
        # 创建优化器
        _logger.info("Creating DSL refiner...")
        refiner = DSLRefiner(llm=llm, input_data=input_data)
        
        # 执行优化
        _logger.info("Starting refinement process...")
        refined_dsl = refiner.refine()
        
        if refined_dsl is None:
            raise RuntimeError("Refinement failed: result is None")
        
        if len(refined_dsl.strip()) == 0:
            raise RuntimeError("Refinement failed: result is empty")
        
        _logger.info("Refinement completed successfully!")
        
        # 保存结果
        _logger.info(f"Saving refined DSL to {self.refined_dsl_path}")
        with open(self.refined_dsl_path, "w", encoding="utf-8") as f:
            f.write(refined_dsl)
        _logger.info(f"✓ Refined DSL saved")
        
        # 保存上下文（用于后续调试）
        _logger.info(f"Saving context to {self.context_path}")
        refiner.serialize_context(self.context_path)
        _logger.info(f"✓ Context saved for debugging")
        
        _logger.info("=" * 60)
        _logger.info("Full refinement process completed!")
        _logger.info("=" * 60)
    
    def run_from_construct(self):
        """
        从构造 DSL 步骤开始调试（不需要 LLM 调用）
        
        需要先运行 run_full_refinement() 生成 context.json 文件。
        """
        _logger.info("=" * 60)
        _logger.info("Running refinement from CONSTRUCT_DSL step (without LLM)")
        _logger.info("=" * 60)
        
        # 检查测试数据文件
        if not self.check_test_data_files():
            raise FileNotFoundError("Test data files not found")
        
        # 检查 context 文件是否存在
        if not self.context_path.exists():
            # 尝试从其他可能的位置加载
            possible_locations = [
                Path("exp/thesis/refine/output/refine_context.json"),  # 默认位置
                Path(__file__).parent.parent.parent / "tests" / "refine" / "refine_output" / "refine_context.json",  # 旧测试文件位置
            ]
            
            for location in possible_locations:
                if location.exists():
                    self.context_path = location
                    _logger.info(f"Using context from: {self.context_path}")
                    break
            else:
                raise FileNotFoundError(
                    f"Context file not found. Checked:\n"
                    f"  - {self.context_path}\n"
                    f"  - {possible_locations[0]}\n"
                    f"  - {possible_locations[1]}\n"
                    "Please run with --full first to generate the context file."
                )
        
        # 加载输入数据
        _logger.info("Loading input data...")
        input_data = load_refine_input_from_paths(
            dsl_path=self.test_data['dsl_path'],
            buggy_path=self.test_data['buggy_path'],
            fixed_path=self.test_data['fixed_path'],
            root_cause_path=self.test_data['root_cause_path'],
            fp_results_path=self.test_data['fp_results_path']
        )
        
        _logger.info("Input data loaded successfully")
        _logger.info(f"Loading context from {self.context_path} and starting from construct step")
        
        # 创建优化器（不需要 LLM）
        _logger.info("Creating DSL refiner (without LLM)...")
        refiner = DSLRefiner(llm=None, input_data=input_data)
        
        # 加载上下文
        _logger.info("Loading context...")
        refiner.load_context_from_json(self.context_path)
        
        # 从构造步骤开始执行
        _logger.info("Starting from CONSTRUCT_DSL step...")
        refiner.start_from_step(RefineStep.CONSTRUCT_DSL, input_data=input_data)
        
        # 执行优化（只执行构造 DSL 步骤）
        _logger.info("Running refinement (construct DSL step only)...")
        refined_dsl = refiner.refine()
        
        if refined_dsl is None:
            raise RuntimeError("Refinement failed: result is None")
        
        if len(refined_dsl.strip()) == 0:
            raise RuntimeError("Refinement failed: result is empty")
        
        _logger.info("Refinement completed successfully!")
        
        # 保存结果
        _logger.info(f"Saving refined DSL to {self.refined_dsl_path}")
        with open(self.refined_dsl_path, "w", encoding="utf-8") as f:
            f.write(refined_dsl)
        _logger.info(f"✓ Refined DSL saved")
        
        _logger.info("=" * 60)
        _logger.info("Refinement from CONSTRUCT_DSL step completed!")
        _logger.info("=" * 60)


def main():
    """主函数"""
    parser = argparse.ArgumentParser(
        description="DSL Refiner 单实例调试脚本",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
        示例:
        # 运行完整流程（包含 LLM 调用）
        python debug/refine/debug_refiner_single_instance.py --full
        
        # 从构造 DSL 步骤开始（不需要 LLM）
        python debug/refine/debug_refiner_single_instance.py --from-construct
                """
    )
    
    parser.add_argument(
        '--full',
        action='store_true',
        help='运行完整流程（包含 LLM 调用）'
    )
    
    parser.add_argument(
        '--from-construct',
        action='store_true',
        help='从构造 DSL 步骤开始（不需要 LLM，需要先运行 --full 生成 context）'
    )
    
    args = parser.parse_args()
    
    # 默认使用 --full
    if not args.full and not args.from_construct:
        args.full = True
        _logger.info("No mode specified, using --full by default")
    
    try:
        debugger = RefinerDebugger()
        
        if args.full:
            debugger.run_full_refinement()
        elif args.from_construct:
            debugger.run_from_construct()
        return 0
        
    except KeyboardInterrupt:
        _logger.warning("Debug session interrupted by user")
        return 130
    except Exception as e:
        _logger.error(f"Error during debug session: {e}", exc_info=True)
        return 1


if __name__ == "__main__":
    sys.exit(main())
