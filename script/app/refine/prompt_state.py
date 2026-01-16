"""
DSL优化框架的状态机实现
"""
import re
import copy
from typing import Optional
from app.refine.data_structures import LLMContext, ExtraConstraint, RefineInput, RefineStep, ConstraintType
from app.refine.prompts import (
    ANALYZE_DSL_PROMPT,
    ANALYZE_FP_PROMPT,
    EXTRACT_CONSTRAINT_PROMPT,
    VALIDATE_CONSTRAINT_PROMPT,
    DSL_GRAMMAR_PROMPT
)
from app.refine.dsl_constructor import merge_constraints_to_dsl, constraint_to_condition, condition_to_dsl
from app.refine.parser import DSLParser
from app.refine.parser.dsl_validator import DSLValidator
from app.refine.parser.dsl_fix_suggester import DSLFixSuggester
import json
from interface.llm.llm_api import LLMAPI
from utils.common import retry_times, valid_with
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


class PromptState:
    """状态基类"""
    def __init__(self, refiner):
        self.refiner = refiner

    def accept(self):
        """状态处理逻辑，子类必须实现"""
        raise NotImplementedError
    
    def add_user_message(self, step: RefineStep, prompt: str, messages: list):
        """
        辅助方法：添加用户消息到消息列表和上下文历史
        
        Args:
            step: 步骤枚举
            prompt: 提示内容
            messages: 消息列表（会被修改）
        """
        user_message = {"role": "user", "content": prompt}
        messages.append(user_message)
        self.refiner.context.add_message(step, "user", prompt)
        return user_message
    
    def add_assistant_message(self, step: RefineStep, response: str, messages: list):
        """
        辅助方法：添加助手消息到上下文历史
        
        Args:
            step: 步骤枚举
            response: 响应内容
            messages: 消息列表（可选，如果提供会被修改）
        """
        assistant_message = {"role": "assistant", "content": response}
        if messages is not None:
            messages.append(assistant_message)
        self.refiner.context.add_message(step, "assistant", response)
        return assistant_message


class InitialState(PromptState):
    """初始状态"""
    def accept(self):
        self.refiner.prompt_state = AnalyzeDSLState(self.refiner)


class ExitState(PromptState):
    """退出状态"""
    def accept(self):
        pass


class AnalyzeDSLState(PromptState):
    """Step1: 分析DSL数据"""
    pattern = re.compile(
        r'\[DSL_ANALYSIS\](.*?)\[/DSL_ANALYSIS\]',
        re.DOTALL | re.IGNORECASE
    )

    def check_valid(self, response: str) -> bool:
        match = re.search(self.pattern, response)
        return bool(match)

    @retry_times(retries=5)
    @valid_with(check_valid)
    def invoke_validate_retry(self, messages: list) -> str:
        return self.refiner.llm.invoke(messages)

    def extract_analysis(self, response: str) -> Optional[str]:
        match = re.search(self.pattern, response)
        if match:
            return match.group(1).strip()
        return None

    def accept(self):
        # Step1: 获取累积历史（此时还没有历史，所以是空列表）
        messages = self.refiner.context.get_accumulated_history(up_to_step=RefineStep.ANALYZE_DSL)
        
        prompt = ANALYZE_DSL_PROMPT.format(
            dsl_grammar=DSL_GRAMMAR_PROMPT,
            dsl_code=self.refiner.input_data.dsl_code,
            buggy_code=self.refiner.input_data.buggy_code,
            fixed_code=self.refiner.input_data.fixed_code,
            root_cause=self.refiner.input_data.root_cause
        )
        
        # 添加当前步骤的用户消息
        self.add_user_message(RefineStep.ANALYZE_DSL, prompt, messages)
        
        try:
            success, response = self.invoke_validate_retry(messages)
            if not success:
                _logger.error("Failed to get valid response after retries")
                self.refiner.prompt_state = ExitState(self.refiner)
                return
            self.add_assistant_message(RefineStep.ANALYZE_DSL, response, messages)
            analysis_result = self.extract_analysis(response)
            if analysis_result:
                self.refiner.context.dsl_analysis_result = analysis_result
                self.refiner.prompt_state = AnalyzeFPState(self.refiner)
            else:
                _logger.error("Failed to extract DSL analysis result")
                self.refiner.prompt_state = ExitState(self.refiner)
        except Exception as e:
            _logger.error(f"Error in AnalyzeDSLState: {e}")
            self.refiner.prompt_state = ExitState(self.refiner)


class AnalyzeFPState(PromptState):
    """Step2: 分析FP原因"""
    pattern = re.compile(
        r'\[FP_ANALYSIS\](.*?)\[/FP_ANALYSIS\]',
        re.DOTALL | re.IGNORECASE
    )

    def check_valid(self, response: str) -> bool:
        match = re.search(self.pattern, response)
        return bool(match)

    @retry_times(retries=5)
    @valid_with(check_valid)
    def invoke_validate_retry(self, messages: list) -> str:
        return self.refiner.llm.invoke(messages)

    def extract_analysis(self, response: str) -> Optional[str]:
        match = re.search(self.pattern, response)
        if match:
            return match.group(1).strip()
        return None
    
    def extract_scenario(self, response: str) -> Optional[int]:
        """
        从FP分析结果中提取Scenario编号
        使用强大的正则表达式统一处理多种格式变体
        """
        # 统一的正则表达式：匹配 "Scenario: 1"、"Scenario 1"、"1: Scenario"、"1 Scenario" 等格式
        # 大小写不敏感，冒号可选，支持前后位置
        # 使用两个捕获组：第一个匹配 "Scenario: 数字"，第二个匹配 "数字: Scenario"
        pattern = r'(?i)(?:scenario\s*:?\s*(\d+)|(\d+)\s*:?\s*scenario)'
        
        scenario_match = re.search(pattern, response)
        if scenario_match:
            # 获取第一个非None的捕获组
            scenario_num_str = scenario_match.group(1) or scenario_match.group(2)
            if scenario_num_str:
                try:
                    scenario_num = int(scenario_num_str)
                    # 验证是1或2
                    if scenario_num in [1, 2]:
                        return scenario_num
                except ValueError:
                    pass
        
        # 如果正则匹配失败，尝试从关键词推断（作为最后的回退方案）
        # 注意：不再检查 "scenario 1/2" 格式，因为正则已经覆盖
        response_lower = response.lower()
        # 仅通过关键词推断scenario类型
        if any(keyword in response_lower for keyword in ['over-general', 'overgeneral', 'too general', 'over-generalized']):
            return 1
        if any(keyword in response_lower for keyword in ['partial', 'missing', 'incomplete', 'partially']):
            return 2
        
        _logger.warning("Could not extract scenario number from FP analysis")
        return None

    def accept(self):
        # Step2: 获取累积历史（包含Step1的完整对话）
        # get_accumulated_history 已经返回深拷贝，无需再次 deepcopy
        messages = self.refiner.context.get_accumulated_history(up_to_step=RefineStep.ANALYZE_FP)
        
        prompt = ANALYZE_FP_PROMPT.format(
            fp_code=self.refiner.input_data.fp_code
        )
        
        # 添加当前步骤的用户消息
        self.add_user_message(RefineStep.ANALYZE_FP, prompt, messages)
        
        try:
            success, response = self.invoke_validate_retry(messages)
            if not success:
                _logger.error("Failed to get valid response after retries")
                self.refiner.prompt_state = ExitState(self.refiner)
                return
            self.add_assistant_message(RefineStep.ANALYZE_FP, response, messages)
            fp_analysis = self.extract_analysis(response)
            if fp_analysis:
                self.refiner.context.fp_analysis_result = fp_analysis
                # 提取Scenario信息
                scenario = self.extract_scenario(response)
                if scenario:
                    self.refiner.context.fp_scenario = scenario
                self.refiner.prompt_state = ExtractConstraintState(self.refiner)
            else:
                _logger.error("Failed to extract FP analysis result")
                self.refiner.prompt_state = ExitState(self.refiner)
        except Exception as e:
            _logger.error(f"Error in AnalyzeFPState: {e}")
            self.refiner.prompt_state = ExitState(self.refiner)


class ExtractConstraintState(PromptState):
    """Step3: 提取额外约束"""
    pattern = re.compile(
        r'\[CONSTRAINTS\](.*?)\[/CONSTRAINTS\]',
        re.DOTALL | re.IGNORECASE
    )

    def check_valid(self, response: str) -> bool:
        match = re.search(self.pattern, response)
        return bool(match)

    @retry_times(retries=5)
    @valid_with(check_valid)
    def invoke_validate_retry(self, messages: list) -> str:
        return self.refiner.llm.invoke(messages)

    def parse_constraints(self, response: str, source_type: str) -> list[ExtraConstraint]:
        """
        解析约束文本，提取ExtraConstraint对象
        
        新格式支持：
        - Type: add/edit/del
        - Path: nodeAlias.attribute
        - Operator: 操作符（add/edit 必需，del 可选）
        - Value: 值（add/edit 必需，del 可选）
        - Original Value: 原始值（仅 edit 类型，可选）
        - Is Negative: yes/no/true/false
        """
        match = re.search(self.pattern, response)
        if not match:
            _logger.warning("No [CONSTRAINTS] block found in response")
            return []
        
        constraints_text = match.group(1).strip()
        
        # 如果约束块为空，返回空列表
        if not constraints_text or constraints_text.strip() == "":
            _logger.info("Empty constraints block found")
            return []
        
        constraints = []
        
        # 分割约束块：支持多种格式
        # 1. "Constraint N:" 格式
        # 2. 空行分割
        # 3. 以 "-" 开头的列表格式
        constraint_blocks = re.split(r'Constraint\s+\d+\s*:', constraints_text, flags=re.IGNORECASE)
        
        # 如果分割失败，尝试按空行分割
        if len(constraint_blocks) == 1 and constraint_blocks[0] == constraints_text:
            constraint_blocks = re.split(r'\n\s*\n', constraints_text)
        
        # 如果还是只有一个块，尝试按 "- Type:" 或 "- Path:" 分割
        if len(constraint_blocks) == 1 and constraint_blocks[0] == constraints_text:
            # 查找所有以 "-" 开头的行作为分隔符
            lines = constraints_text.split('\n')
            current_block = []
            for line in lines:
                if re.match(r'^\s*-\s*(Type|Path)\s*[:|-]', line, re.IGNORECASE) and current_block:
                    constraint_blocks.append('\n'.join(current_block))
                    current_block = [line]
                else:
                    current_block.append(line)
            if current_block:
                constraint_blocks.append('\n'.join(current_block))
        
        for block_idx, block in enumerate(constraint_blocks, 1):
            block = block.strip()
            if not block:
                continue
            
            # 辅助函数：提取字段值，支持多种格式变体
            def extract_field(block: str, field_name: str, required: bool = False) -> Optional[str]:
                """提取字段值，支持 'Field:', 'Field -', '- Field:', '- Field -' 等格式"""
                # 改进的pattern：匹配到行尾或下一个以 "-" 开头的字段行
                patterns = [
                    # 匹配 "- Field: value" 格式，直到行尾或下一个 "-" 开头的行
                    # 使用更精确的匹配：匹配到换行符或下一个 "- Field:" 模式
                    rf'-\s*{re.escape(field_name)}\s*:\s*([^\n]+?)(?=\s*$|\s*\n\s*-\s*(?:Type|Path|Operator|Value|Original\s+Value|Is\s+Negative)\s*[:|-])',
                    # 匹配 "Field: value" 格式（行首）
                    rf'^{re.escape(field_name)}\s*:\s*([^\n]+?)(?=\s*$|\s*\n\s*-\s*(?:Type|Path|Operator|Value|Original\s+Value|Is\s+Negative)\s*[:|-])',
                    # 匹配 "- Field - value" 格式
                    rf'-\s*{re.escape(field_name)}\s*-\s*([^\n]+)',
                    # 匹配 "Field - value" 格式
                    rf'{re.escape(field_name)}\s*-\s*([^\n]+)',
                    # 兜底：匹配到行尾（如果上面的模式都失败）
                    rf'{re.escape(field_name)}\s*:\s*([^\n]+)',
                ]
                
                for pattern in patterns:
                    match = re.search(pattern, block, re.IGNORECASE | re.MULTILINE | re.DOTALL)
                    if match:
                        value = match.group(1).strip()
                        # 清理值：移除末尾可能存在的字段名残留（但要小心，不要误删路径中的点）
                        # 只在值末尾是字段名时才删除（避免删除路径中的点）
                        value = re.sub(r'\s+(Type|Path|Operator|Value|Original\s+Value|Is\s+Negative)\s*:?\s*$', '', value, flags=re.IGNORECASE)
                        return value.strip()
                
                if required:
                    _logger.warning(f"Constraint {block_idx}: Missing required field '{field_name}'")
                return None
            
            # 提取 Type 字段（必需）
            type_str = extract_field(block, "Type", required=True)
            if not type_str:
                _logger.warning(f"Constraint {block_idx}: Missing Type field, skipping")
                continue
            
            # 解析类型
            type_str_lower = type_str.lower().strip()
            if type_str_lower in ['add', 'a']:
                constraint_type = ConstraintType.ADD
            elif type_str_lower in ['edit', 'e', 'modify', 'update']:
                constraint_type = ConstraintType.EDIT
            elif type_str_lower in ['del', 'delete', 'remove', 'd']:
                constraint_type = ConstraintType.DEL
            else:
                _logger.warning(f"Constraint {block_idx}: Invalid Type '{type_str}', expected 'add', 'edit', or 'del'. Defaulting to 'add'")
                constraint_type = ConstraintType.ADD
            
            # 提取 Path 字段（必需）
            path_str = extract_field(block, "Path", required=True)
            if not path_str:
                continue
            constraint_path = path_str.strip()
            
            # 验证和清理路径格式
            # 路径格式应该是 "nodeAlias.attribute" 或 "nodeAlias.attribute.subattribute"
            # 不能以点结尾
            if constraint_path.endswith('.'):
                _logger.warning(f"Constraint {block_idx}: Path ends with '.', removing trailing dot: '{constraint_path}'")
                constraint_path = constraint_path.rstrip('.')
            
            # 验证路径格式：应该包含至少一个点（nodeAlias.attribute）
            if '.' not in constraint_path:
                _logger.warning(f"Constraint {block_idx}: Invalid path format (should be 'nodeAlias.attribute'): '{constraint_path}'")
                # 尝试修复：如果只有 nodeAlias，可能需要添加默认属性
                # 但这里我们不知道应该添加什么属性，所以记录警告但继续处理
            
            # 根据类型提取不同字段
            operator = None
            value = None
            original_value = None
            
            if constraint_type in [ConstraintType.ADD, ConstraintType.EDIT]:
                # ADD 和 EDIT 需要 Operator 和 Value
                operator = extract_field(block, "Operator", required=True)
                if not operator:
                    _logger.warning(f"Constraint {block_idx}: Missing Operator field for {constraint_type.value} type")
                    continue
                
                value = extract_field(block, "Value", required=True)
                if not value:
                    _logger.warning(f"Constraint {block_idx}: Missing Value field for {constraint_type.value} type")
                    continue
                
                # 清理值（处理多行和子查询）
                if ';' in value:
                    # 可能是子查询，保留基本格式但清理多余空格
                    value = re.sub(r'\n\s+', ' ', value)
                else:
                    # 单行值，合并空格
                    value = re.sub(r'\s+', ' ', value)
                
                # EDIT 类型可能包含 Original Value
                if constraint_type == ConstraintType.EDIT:
                    original_value = extract_field(block, "Original Value", required=False)
                    if original_value:
                        original_value = original_value.strip()
            
            # 提取 Is Negative 字段（可选）
            negative_str = extract_field(block, "Is Negative", required=False)
            is_negative = False
            if negative_str:
                negative_str_lower = negative_str.lower().strip()
                is_negative = negative_str_lower in ['yes', 'true', 'y', '1']
            else:
                # 默认根据 source_type 判断
                is_negative = (source_type == "fp")
                if constraint_type != ConstraintType.DEL:
                    _logger.debug(f"Constraint {block_idx}: Missing Is Negative field, defaulting to {'yes' if is_negative else 'no'}")
            
            # 创建约束对象
            try:
                constraint = ExtraConstraint(
                    constraint_path=constraint_path,
                    operator=operator or "",  # DEL 类型可能为空
                    value=value or "",  # DEL 类型可能为空
                    constraint_type=constraint_type,
                    is_negative=is_negative,
                    source_file=source_type,
                    original_value=original_value
                )
                
                constraints.append(constraint)
                _logger.debug(f"Successfully parsed constraint {block_idx}: type={constraint_type.value}, path={constraint_path}")
            except Exception as e:
                _logger.error(f"Constraint {block_idx}: Failed to create constraint object: {e}")
                continue
        
        if not constraints:
            _logger.warning("No valid constraints extracted from response")
            return []
        
        # 去重：移除重复的约束
        # 第一步：移除完全相同的约束（相同的 path、type、operator 和 value）
        seen_constraints = set()
        unique_constraints = []
        
        for constraint in constraints:
            # 创建唯一标识符
            constraint_key = (
                constraint.constraint_path,
                constraint.constraint_type.value,
                constraint.operator,
                constraint.value
            )
            
            if constraint_key not in seen_constraints:
                seen_constraints.add(constraint_key)
                unique_constraints.append(constraint)
            else:
                _logger.debug(f"Removed duplicate constraint: path={constraint.constraint_path}, type={constraint.constraint_type.value}")
        
        # 第二步：对于同一 path 的多个相同类型的约束，只保留第一个
        constraint_by_path_type = {}  # (path, type) -> list of constraints
        for constraint in unique_constraints:
            key = (constraint.constraint_path, constraint.constraint_type)
            if key not in constraint_by_path_type:
                constraint_by_path_type[key] = []
            constraint_by_path_type[key].append(constraint)
        
        deduplicated_constraints = []
        for (path, constraint_type), path_type_constraints in constraint_by_path_type.items():
            if len(path_type_constraints) == 1:
                deduplicated_constraints.append(path_type_constraints[0])
            else:
                # 同一 path 和 type 有多个约束，只保留第一个
                deduplicated_constraints.append(path_type_constraints[0])
                _logger.warning(f"Multiple {constraint_type.value} constraints for path '{path}', keeping the first one")
        
        if len(deduplicated_constraints) < len(constraints):
            _logger.info(f"Removed {len(constraints) - len(deduplicated_constraints)} duplicate constraints")
        
        return deduplicated_constraints

    def accept(self):
        # Step3: 获取累积历史（包含Step1和Step2的完整对话）
        # get_accumulated_history 已经返回深拷贝，无需再次 deepcopy
        base_messages = self.refiner.context.get_accumulated_history(up_to_step=RefineStep.EXTRACT_CONSTRAINT)
        
        # 根据FP分析结果中的Scenario，决定从哪个文件提取约束
        scenario = self.refiner.context.fp_scenario
        
        # 根据Scenario确定提取策略
        # 每个scenario只从最相关的单个文件提取约束
        if scenario == 1:
            # Scenario 1: DSL well describes root cause but over-generalized
            # 从FP代码提取负约束，用于过滤误报
            source_files = [
                ("fp", self.refiner.input_data.fp_code)
            ]
        elif scenario == 2:
            # Scenario 2: DSL only partially captures buggy_code structure
            # 从buggy代码提取缺失的正约束，用于补充DSL
            source_files = [
                ("buggy", self.refiner.input_data.buggy_code)
            ]
        else:
            # 默认情况：从buggy代码提取（最常用的场景）
            source_files = [
                ("buggy", self.refiner.input_data.buggy_code)
            ]
        
        all_constraints = []
        
        for source_type, source_code in source_files:
            prompt = EXTRACT_CONSTRAINT_PROMPT.format(
                original_dsl=self.refiner.input_data.dsl_code,
                source_code=source_code,
                source_type=source_type
            )
            
            # get_accumulated_history 已经返回深拷贝，直接使用即可
            # 每次调用都基于累积历史，并添加当前请求
            current_messages = base_messages.copy()  # 浅拷贝列表即可，因为列表内的字典已经是深拷贝
            self.add_user_message(RefineStep.EXTRACT_CONSTRAINT, prompt, current_messages)
            
            try:
                success, response = self.invoke_validate_retry(current_messages)
                if not success:
                    _logger.warning(f"Failed to get valid response after retries for {source_type}")
                    continue
                self.add_assistant_message(RefineStep.EXTRACT_CONSTRAINT, response, current_messages)
                constraints = self.parse_constraints(response, source_type)
                all_constraints.extend(constraints)
                
                # 将本次对话添加到累积历史中，供下次调用使用（如果有多个文件）
                base_messages.extend(current_messages[-2:])  # 添加最后两个消息（user和assistant）
            except Exception as e:
                _logger.warning(f"Failed to extract constraints from {source_type}: {e}")
        
        if all_constraints:
            self.refiner.context.extracted_constraints = all_constraints
            self.refiner.prompt_state = ValidateConstraintState(self.refiner)
        else:
            _logger.warning("No constraints extracted, proceeding to construct DSL with original")
            self.refiner.prompt_state = ConstructDSLState(self.refiner)


class ValidateConstraintState(PromptState):
    """Step3.5: 验证和修复约束"""
    pattern = re.compile(
        r'\[CONSTRAINTS\](.*?)\[/CONSTRAINTS\]',
        re.DOTALL | re.IGNORECASE
    )
    
    MAX_RETRIES = 3
    
    def check_valid(self, response: str) -> bool:
        match = re.search(self.pattern, response)
        return bool(match)
    
    @retry_times(retries=5)
    @valid_with(check_valid)
    def invoke_validate_retry(self, messages: list) -> tuple[bool, str]:
        response = self.refiner.llm.invoke(messages)
        success = self.check_valid(response)
        return success, response
    
    def validate_constraint(self, constraint: ExtraConstraint, original_dsl: str) -> tuple[bool, Optional[str], Optional[str]]:
        """
        验证单个约束的合法性
        
        Args:
            constraint: 要验证的约束
            original_dsl: 原始DSL代码（用于获取节点类型信息）
        
        Returns:
            (is_valid, error_message, fix_suggestion)
        """
        try:
            # 将约束转换为Condition AST
            condition = constraint_to_condition(constraint)
            if not condition:
                return False, "Failed to convert constraint to Condition AST", None
            
            # 将Condition转换为DSL子条件字符串
            condition_dsl = condition_to_dsl(condition)
            if not condition_dsl:
                return False, "Failed to convert condition to DSL string", None
            
            # 需要构建一个完整的DSL查询来验证
            # 从原始DSL中解析出节点类型和别名映射
            parser = DSLParser(original_dsl)
            original_query = parser.parse()
            if not original_query:
                return False, "Failed to parse original DSL", None
            
            # 从constraint_path中提取别名
            path_parts = constraint.constraint_path.split('.')
            if not path_parts:
                return False, "Invalid constraint_path format", None
            
            alias = path_parts[0]
            
            # 查找该别名对应的节点类型
            node_type = None
            if hasattr(parser, 'node_map') and alias in parser.node_map:
                node_type = parser.node_map[alias].entity.node_type
            else:
                # 尝试从原始查询中查找
                if original_query.entity.alias == alias:
                    node_type = original_query.entity.node_type
                else:
                    # 递归查找嵌套查询
                    def find_node_type_in_condition(cond, target_alias):
                        if cond.atomic:
                            if cond.atomic.rel_match and cond.atomic.rel_match.query:
                                if cond.atomic.rel_match.query.entity.alias == target_alias:
                                    return cond.atomic.rel_match.query.entity.node_type
                                result = find_node_type_in_condition(cond.atomic.rel_match.query.condition, target_alias)
                                if result:
                                    return result
                        for sub_cond in cond.sub_conditions:
                            result = find_node_type_in_condition(sub_cond, target_alias)
                            if result:
                                return result
                        return None
                    
                    node_type = find_node_type_in_condition(original_query.condition, alias)
            
            if not node_type:
                return False, f"Cannot find node type for alias '{alias}' in original DSL", None
            
            # 构建一个临时查询用于验证
            from app.refine.parser import EntityDecl, Query
            temp_entity = EntityDecl(node_type=node_type, alias=alias)
            temp_query = Query(
                entity=temp_entity,
                condition=condition,
                start_pos=0,
                end_pos=0,
                where_start=0,
                where_end=0,
                condition_start=0,
                condition_end=0
            )
            
            # 将临时查询转换为DSL
            temp_dsl = f"{node_type} {alias} where {condition_dsl} ;"
            
            # 解析并验证
            temp_parser = DSLParser(temp_dsl)
            temp_parsed = temp_parser.parse()
            if not temp_parsed:
                parse_errors = temp_parser.get_parse_errors()
                error_msg = "; ".join(parse_errors) if parse_errors else "Parse failed"
                return False, f"Syntax error: {error_msg}", None
            
            # 语义验证
            validator = DSLValidator()
            validation_result = validator.validate(temp_parsed)
            
            if not validation_result.is_valid:
                error_messages = [e.message for e in validation_result.errors]
                fix_suggestions = DSLFixSuggester.generate_fix_message(validation_result.errors)
                return False, "; ".join(error_messages), fix_suggestions
            
            return True, None, None
            
        except Exception as e:
            _logger.error(f"Error validating constraint: {e}", exc_info=True)
            return False, f"Validation error: {str(e)}", None
    
    def validate_all_constraints(self, constraints: list[ExtraConstraint], original_dsl: str) -> tuple[list[int], list[str], list[str]]:
        """
        验证所有约束，返回无效约束的索引、错误消息和修复建议
        
        Returns:
            (invalid_indices, error_messages, fix_suggestions)
        """
        invalid_indices = []
        error_messages = []
        fix_suggestions = []
        
        for idx, constraint in enumerate(constraints):
            is_valid, error_msg, fix_suggestion = self.validate_constraint(constraint, original_dsl)
            if not is_valid:
                invalid_indices.append(idx)
                error_messages.append(f"Constraint {idx + 1} ({constraint.constraint_path}): {error_msg}")
                if fix_suggestion:
                    fix_suggestions.append(f"Constraint {idx + 1}:\n{fix_suggestion}")
        
        return invalid_indices, error_messages, fix_suggestions
    
    def constraints_to_json(self, constraints: list[ExtraConstraint]) -> str:
        """将约束列表转换为JSON格式字符串"""
        constraints_data = []
        for idx, c in enumerate(constraints):
            constraints_data.append({
                "index": idx + 1,
                "type": c.constraint_type.value,
                "path": c.constraint_path,
                "operator": c.operator,
                "value": c.value,
                "is_negative": c.is_negative,
                "original_value": c.original_value
            })
        return json.dumps(constraints_data, indent=2, ensure_ascii=False)
    
    def parse_constraints(self, response: str, source_type: str) -> list[ExtraConstraint]:
        """复用ExtractConstraintState的解析逻辑"""
        extract_state = ExtractConstraintState(self.refiner)
        return extract_state.parse_constraints(response, source_type)
    
    def accept(self):
        """验证约束，如果不合法则调用LLM修复，最多重试3次"""
        _logger.info("Validating extracted constraints...")
        
        constraints = self.refiner.context.extracted_constraints
        original_dsl = self.refiner.input_data.dsl_code
        
        if not constraints:
            _logger.info("No constraints to validate, proceeding to construct DSL")
            self.refiner.prompt_state = ConstructDSLState(self.refiner)
            return
        
        retry_count = 0
        
        while retry_count <= self.MAX_RETRIES:
            # 验证所有约束
            invalid_indices, error_messages, fix_suggestions = self.validate_all_constraints(constraints, original_dsl)
            
            if not invalid_indices:
                # 所有约束都有效
                _logger.info("All constraints are valid")
                self.refiner.context.extracted_constraints = constraints
                self.refiner.prompt_state = ConstructDSLState(self.refiner)
                return
            
            # 有无效约束，需要修复
            if retry_count >= self.MAX_RETRIES:
                _logger.warning(f"Failed to fix constraints after {self.MAX_RETRIES} retries. Proceeding with valid constraints only.")
                # 只保留有效的约束
                valid_constraints = [c for idx, c in enumerate(constraints) if idx not in invalid_indices]
                self.refiner.context.extracted_constraints = valid_constraints
                self.refiner.prompt_state = ConstructDSLState(self.refiner)
                return
            
            _logger.info(f"Found {len(invalid_indices)} invalid constraints. Attempting to fix (retry {retry_count + 1}/{self.MAX_RETRIES})...")
            
            # 准备修复提示
            constraints_json = self.constraints_to_json(constraints)
            validation_errors = "\n".join(error_messages)
            fix_suggestions_text = "\n\n".join(fix_suggestions) if fix_suggestions else "No specific fix suggestions available."
            
            # 获取累积历史
            messages = self.refiner.context.get_accumulated_history(up_to_step=RefineStep.VALIDATE_CONSTRAINT)
            
            prompt = VALIDATE_CONSTRAINT_PROMPT.format(
                original_dsl=original_dsl,
                constraints_json=constraints_json,
                validation_errors=validation_errors,
                fix_suggestions=fix_suggestions_text
            )
            
            # 添加当前步骤的用户消息
            self.add_user_message(RefineStep.VALIDATE_CONSTRAINT, prompt, messages)
            
            try:
                success, response = self.invoke_validate_retry(messages)
                if not success:
                    _logger.error("Failed to get valid response after retries")
                    # 只保留有效的约束
                    valid_constraints = [c for idx, c in enumerate(constraints) if idx not in invalid_indices]
                    self.refiner.context.extracted_constraints = valid_constraints
                    self.refiner.prompt_state = ConstructDSLState(self.refiner)
                    return
                
                self.add_assistant_message(RefineStep.VALIDATE_CONSTRAINT, response, messages)
                
                # 解析修复后的约束
                fixed_constraints = self.parse_constraints(response, "fixed")
                
                if fixed_constraints:
                    # 更新约束列表（保持顺序，只替换无效的约束）
                    # 创建新的约束列表
                    new_constraints = []
                    fixed_idx = 0
                    for idx, constraint in enumerate(constraints):
                        if idx in invalid_indices:
                            if fixed_idx < len(fixed_constraints):
                                new_constraints.append(fixed_constraints[fixed_idx])
                                fixed_idx += 1
                            # 如果修复后的约束数量不足，跳过这个无效约束
                        else:
                            new_constraints.append(constraint)
                    
                    constraints = new_constraints
                    retry_count += 1
                else:
                    _logger.warning("Failed to parse fixed constraints from LLM response")
                    # 只保留有效的约束
                    valid_constraints = [c for idx, c in enumerate(constraints) if idx not in invalid_indices]
                    self.refiner.context.extracted_constraints = valid_constraints
                    self.refiner.prompt_state = ConstructDSLState(self.refiner)
                    return
                    
            except Exception as e:
                _logger.error(f"Error in ValidateConstraintState: {e}", exc_info=True)
                # 只保留有效的约束
                valid_constraints = [c for idx, c in enumerate(constraints) if idx not in invalid_indices]
                self.refiner.context.extracted_constraints = valid_constraints
                self.refiner.prompt_state = ConstructDSLState(self.refiner)
                return


class ConstructDSLState(PromptState):
    """Step4: 基于约束手动编辑DSL（不再使用LLM）"""
    
    def accept(self):
        """
        基于提取的约束，手动编辑DSL
        不再调用LLM，而是使用字符串操作直接修改DSL
        """
        _logger.info("Constructing refined DSL based on extracted constraints...")
        
        try:
            # 使用dsl_constructor中的函数直接合并约束到DSL
            refined_dsl = merge_constraints_to_dsl(
                original_dsl=self.refiner.input_data.dsl_code,
                constraints=self.refiner.context.extracted_constraints
            )
            
            if refined_dsl:
                self.refiner.refined_dsl = refined_dsl
                _logger.info("Successfully constructed refined DSL")
                self.refiner.prompt_state = ExitState(self.refiner)
            else:
                _logger.warning("Failed to construct refined DSL, using original DSL")
                self.refiner.refined_dsl = self.refiner.input_data.dsl_code
                self.refiner.prompt_state = ExitState(self.refiner)
        except Exception as e:
            _logger.error(f"Error in ConstructDSLState: {e}")
            # 出错时使用原始DSL
            self.refiner.refined_dsl = self.refiner.input_data.dsl_code
            self.refiner.prompt_state = ExitState(self.refiner)
