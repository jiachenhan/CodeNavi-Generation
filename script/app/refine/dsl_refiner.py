"""
DSL优化框架的主类
"""
import json
from pathlib import Path
from typing import Optional, List

from app.refine.data_structures import RefineInput, LLMContext, RefineStep, ExtraConstraint, ConstraintType
from app.refine.prompt_state import (
    PromptState,
    InitialState,
    ExitState,
    ConstructDSLState
)
from interface.llm.llm_api import LLMAPI
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


class DSLRefiner:
    """
    DSL优化器，基于状态机模式实现DSL refine流程
    """
    
    def __init__(self, llm: Optional[LLMAPI] = None, input_data: Optional[RefineInput] = None, retries: int = 5):
        """
        初始化DSL优化器
        
        Args:
            llm: LLM API接口（可选，如果从中间步骤开始则不需要）
            input_data: 输入数据（可选，如果从中间步骤开始则不需要）
            retries: 重试次数
        """
        self.llm = llm
        self.input_data = input_data
        self.retries = retries
        
        # 状态机
        self.prompt_state: PromptState = InitialState(self)
        
        # 上下文管理
        self.context = LLMContext()
        
        # 输出结果
        self.refined_dsl: Optional[str] = None
    
    def refine(self) -> Optional[str]:
        """
        执行DSL优化流程
        
        Returns:
            优化后的DSL代码，如果失败则返回None
        """
        _logger.info("Starting DSL refine process...")
        
        # 运行状态机直到退出
        while not isinstance(self.prompt_state, ExitState):
            self.prompt_state.accept()
        
        if self.refined_dsl:
            _logger.info("DSL refine completed successfully")
            return self.refined_dsl
        else:
            _logger.error("DSL refine failed")
            return None
    
    def serialize_context(self, output_path: Path):
        """
        序列化上下文到JSON文件
        
        Args:
            output_path: 输出文件路径
        """
        data = {
            "dsl_analysis": self.context.dsl_analysis_result,
            "fp_analysis": self.context.fp_analysis_result,
            "fp_scenario": self.context.fp_scenario,
            "extracted_constraints": [
                {
                    "constraint_path": c.constraint_path,
                    "operator": c.operator,
                    "value": c.value,
                    "constraint_type": c.constraint_type.value,
                    "is_negative": c.is_negative,
                    "source_file": c.source_file,
                    "original_value": c.original_value
                }
                for c in self.context.extracted_constraints
            ],
            "histories": {
                RefineStep.ANALYZE_DSL.value: self.context.analyze_dsl_history,
                RefineStep.ANALYZE_FP.value: self.context.analyze_fp_history,
                RefineStep.EXTRACT_CONSTRAINT.value: self.context.extract_constraint_history,
                RefineStep.VALIDATE_CONSTRAINT.value: self.context.validate_constraint_history,
                RefineStep.CONSTRUCT_DSL.value: self.context.construct_dsl_history,
            },
            "refined_dsl": self.refined_dsl
        }
        
        if not output_path.parent.exists():
            output_path.parent.mkdir(parents=True, exist_ok=True)
        
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        
        _logger.info(f"Context serialized to {output_path}")
    
    def load_context_from_json(self, context_path: Path):
        """
        从JSON文件加载LLM上下文，用于从中间步骤开始调试
        
        Args:
            context_path: 上下文JSON文件路径
        """
        with open(context_path, "r", encoding="utf-8") as f:
            data = json.load(f)
        
        # 加载中间结果
        self.context.dsl_analysis_result = data.get("dsl_analysis")
        self.context.fp_analysis_result = data.get("fp_analysis")
        self.context.fp_scenario = data.get("fp_scenario")
        
        # 加载提取的约束
        constraints_data = data.get("extracted_constraints", [])
        for c_data in constraints_data:
            constraint = ExtraConstraint(
                constraint_path=c_data.get("constraint_path", ""),
                operator=c_data.get("operator", ""),
                value=c_data.get("value", ""),
                constraint_type=ConstraintType(c_data.get("constraint_type", "add")),
                is_negative=c_data.get("is_negative", False),
                source_file=c_data.get("source_file", ""),
                original_value=c_data.get("original_value")
            )
            self.context.extracted_constraints.append(constraint)
        
        # 加载对话历史（可选）
        histories = data.get("histories", {})
        if histories:
            self.context.analyze_dsl_history = histories.get(RefineStep.ANALYZE_DSL.value, [])
            self.context.analyze_fp_history = histories.get(RefineStep.ANALYZE_FP.value, [])
            self.context.extract_constraint_history = histories.get(RefineStep.EXTRACT_CONSTRAINT.value, [])
            self.context.validate_constraint_history = histories.get(RefineStep.VALIDATE_CONSTRAINT.value, [])
            self.context.construct_dsl_history = histories.get(RefineStep.CONSTRUCT_DSL.value, [])
        
        _logger.info(f"Context loaded from {context_path}")
    
    def start_from_step(self, step: RefineStep, input_data: Optional[RefineInput] = None):
        """
        从指定步骤开始执行状态机
        
        Args:
            step: 起始步骤，使用RefineStep枚举
            input_data: 输入数据（如果从construct步骤开始，需要提供原始DSL）
        """
        if step == RefineStep.CONSTRUCT_DSL:
            if not self.context.extracted_constraints:
                raise ValueError("Cannot start from construct step: no constraints loaded. Call load_context_from_json first.")
            if input_data:
                self.input_data = input_data
            if not self.input_data:
                raise ValueError("Cannot start from construct step: input_data is required")
            # 直接设置状态为ConstructDSLState
            self.prompt_state = ConstructDSLState(self)
            _logger.info("Starting from ConstructDSLState")
        else:
            raise ValueError(f"Unsupported start step: {step}. Only CONSTRUCT_DSL is supported for now.")


def load_fp_codes_from_results(
    fp_results_path: Path,
    label_filter: str = "fp",
    code_field: str = "method_source",
    max_count: Optional[int] = None
) -> List[str]:
    """
    从FP结果文件中加载FP代码列表
    
    Args:
        fp_results_path: FP结果文件路径（JSON格式，list[dict]）
        label_filter: 用于过滤的label字段值，默认为"fp"
        code_field: 提取代码的字段名，默认为"method_source"
        max_count: 最大返回数量，None表示返回所有匹配的FP
    
    Returns:
        FP代码列表
    """
    with open(fp_results_path, "r", encoding="utf-8") as f:
        fp_results = json.load(f)
    
    fp_codes = []
    for item in fp_results:
        if item.get("label") == label_filter:
            code = item.get(code_field, "")
            if code:
                fp_codes.append(code)
                if max_count is not None and len(fp_codes) >= max_count:
                    break
    
    return fp_codes


def load_refine_input_from_paths(
    dsl_path: Path,
    buggy_path: Path,
    fixed_path: Path,
    root_cause_path: Path,
    fp_results_path: Path,
    fp_index: int = 0
) -> RefineInput:
    """
    从文件路径加载RefineInput数据
    
    Args:
        dsl_path: DSL文件路径
        buggy_path: buggy代码文件路径
        fixed_path: fixed代码文件路径
        root_cause_path: root_cause信息文件路径（JSON格式，包含'may_be_fixed_violations'字段）
        fp_results_path: FP结果文件路径（JSON格式，list[dict]）
        fp_index: 使用第几个FP（从0开始），默认为0（第一个）
    
    Returns:
        RefineInput对象
    """
    # 加载DSL
    with open(dsl_path, "r", encoding="utf-8") as f:
        dsl_code = f.read()
    
    # 加载buggy代码
    with open(buggy_path, "r", encoding="utf-8") as f:
        buggy_code = f.read()
    
    # 加载fixed代码
    with open(fixed_path, "r", encoding="utf-8") as f:
        fixed_code = f.read()
    
    # 加载root_cause
    with open(root_cause_path, "r", encoding="utf-8") as f:
        root_cause_data = json.load(f)
        root_cause = root_cause_data.get("may_be_fixed_violations", "").strip()
    
    # 加载FP代码（使用解耦的函数）
    fp_codes = load_fp_codes_from_results(fp_results_path)
    fp_code = fp_codes[fp_index] if fp_index < len(fp_codes) else ""
    
    if not fp_code and fp_codes:
        _logger.warning(f"Requested FP index {fp_index} not found, using first FP")
        fp_code = fp_codes[0]
    elif not fp_code:
        _logger.warning("No FP codes found in results file")
    
    return RefineInput(
        dsl_code=dsl_code,
        dsl_path=dsl_path,
        buggy_code=buggy_code,
        fixed_code=fixed_code,
        root_cause=root_cause,
        fp_code=fp_code
    )
