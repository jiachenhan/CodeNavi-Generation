"""
DSL优化框架的数据结构定义
"""
from dataclasses import dataclass, field
from typing import List, Dict, Optional
from pathlib import Path
from enum import Enum


class RefineStep(Enum):
    """DSL优化流程的步骤枚举"""
    ANALYZE_DSL = "analyze_dsl"
    ANALYZE_FP = "analyze_fp"
    EXTRACT_CONSTRAINT = "extract_constraint"
    VALIDATE_CONSTRAINT = "validate_constraint"
    CONSTRUCT_DSL = "construct_dsl"


@dataclass
class RefineInput:
    """DSL优化的输入数据"""
    dsl_code: str  # DSL代码内容
    buggy_code: str  # 缺陷代码
    fixed_code: str  # 修复后的代码
    root_cause: str  # 缺陷原因描述
    fp_code: str  # 误报代码
    dsl_path: Optional[Path] = None  # DSL文件路径（可选）



class ConstraintType(Enum):
    """约束修改类型枚举"""
    ADD = "add"  # 增加新约束
    EDIT = "edit"  # 修改现有约束的值
    DEL = "del"  # 删除现有约束


@dataclass
class ExtraConstraint:
    """
    额外约束的数据结构
    
    约束位置使用精确路径：node_alias.attribute_path
    例如：funcCall.body 或 ifBlock.condition
    """
    constraint_path: str  # 约束位置：格式为 "node_alias.attribute_path"（如 "funcCall.body"）
    operator: str  # 操作符：match, is, ==, !=, contain, in
    value: str  # 值或子查询（如果是子查询，包含完整的DSL子查询）
    constraint_type: ConstraintType = ConstraintType.ADD  # 修改类型：add/edit/del
    is_negative: bool = False  # 是否为负约束（用于过滤FP）
    source_file: str = ""  # 约束来源：buggy, fixed, fp（用于调试）
    # 对于 EDIT 类型，可能需要指定原始值（用于定位要修改的约束）
    original_value: Optional[str] = None  # 原始值（仅用于 EDIT 类型，用于定位要修改的约束）


@dataclass
class LLMContext:
    """LLM对话上下文管理"""
    # 各步骤的对话历史
    analyze_dsl_history: List[Dict] = field(default_factory=list)
    analyze_fp_history: List[Dict] = field(default_factory=list)
    extract_constraint_history: List[Dict] = field(default_factory=list)
    validate_constraint_history: List[Dict] = field(default_factory=list)
    construct_dsl_history: List[Dict] = field(default_factory=list)
    
    # 中间结果
    dsl_analysis_result: Optional[str] = None  # Step1的分析结果
    fp_analysis_result: Optional[str] = None  # Step2的分析结果
    fp_scenario: Optional[int] = None  # FP分析中的Scenario (1 or 2)
    extracted_constraints: List[ExtraConstraint] = field(default_factory=list)  # Step3提取的约束
    
    def _get_history_list(self, step: RefineStep) -> Optional[List[Dict]]:
        """获取指定步骤对应的历史列表（内部方法）"""
        if step == RefineStep.ANALYZE_DSL:
            return self.analyze_dsl_history
        elif step == RefineStep.ANALYZE_FP:
            return self.analyze_fp_history
        elif step == RefineStep.EXTRACT_CONSTRAINT:
            return self.extract_constraint_history
        elif step == RefineStep.VALIDATE_CONSTRAINT:
            return self.validate_constraint_history
        elif step == RefineStep.CONSTRUCT_DSL:
            return self.construct_dsl_history
        else:
            return None
    
    def add_message(self, step: RefineStep, role: str, content: str):
        """添加消息到指定步骤的历史"""
        history_list = self._get_history_list(step)
        if history_list is not None:
            history_list.append({"role": role, "content": content})
    
    def get_history(self, step: RefineStep) -> List[Dict]:
        """获取指定步骤的完整历史"""
        history_list = self._get_history_list(step)
        return history_list if history_list is not None else []
    
    def get_accumulated_history(self, up_to_step: Optional[RefineStep] = None) -> List[Dict]:
        """
        获取累积的完整对话历史（按步骤顺序拼接）
        
        注意：返回的列表和其中的字典都是深拷贝，可以安全修改而不会影响原始数据。
        
        Args:
            up_to_step: 获取到哪个步骤为止的历史，None表示获取所有历史
                - RefineStep.ANALYZE_DSL: 只包含Step1
                - RefineStep.ANALYZE_FP: 包含Step1和Step2
                - RefineStep.EXTRACT_CONSTRAINT: 包含Step1、Step2和Step3
                - RefineStep.CONSTRUCT_DSL: 包含所有步骤
        
        Returns:
            完整的对话历史列表（深拷贝），格式符合OpenAI API要求
        """
        import copy
        
        accumulated = []
        
        # 按步骤顺序添加历史
        steps_order = [
            RefineStep.ANALYZE_DSL,
            RefineStep.ANALYZE_FP,
            RefineStep.EXTRACT_CONSTRAINT,
            RefineStep.VALIDATE_CONSTRAINT,
            RefineStep.CONSTRUCT_DSL
        ]
        
        for step in steps_order:
            step_history = self.get_history(step)
            if step_history:
                # 深拷贝每个消息字典，避免修改原始数据
                accumulated.extend(copy.deepcopy(step_history))
            
            # 如果指定了up_to_step，到达该步骤后停止
            if up_to_step and step == up_to_step:
                break
        
        return accumulated
