"""
ExtractConstraintState - 约束提取状态

负责从LLM响应中提取ExtraConstraint对象。
包含完整的解析逻辑和辅助类。
"""
import re
from typing import Optional, List
from dataclasses import dataclass

from app.refine.states.base_state import PromptState
from app.refine.data_structures import ExtraConstraint, ConstraintType, RefineStep
from app.refine.prompts import get_extract_constraint_prompt
from utils.common import retry_times, valid_with
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


# ============================================================================
# 辅助类 - 字段提取器
# ============================================================================

@dataclass
class FieldExtractor:
    """字段提取器 - 负责从文本块中提取特定字段的值"""

    # 支持的字段格式模式
    FIELD_PATTERNS = [
        # 匹配 "- Field: value" 格式，直到行尾或下一个字段
        r'-\s*{field}\s*:\s*([^\n]+?)(?=\s*$|\s*\n\s*-\s*(?:{all_fields})\s*[:|-])',
        # 匹配 "Field: value" 格式（行首）
        r'^{field}\s*:\s*([^\n]+?)(?=\s*$|\s*\n\s*-\s*(?:{all_fields})\s*[:|-])',
        # 匹配 "- Field - value" 格式
        r'-\s*{field}\s*-\s*([^\n]+)',
        # 匹配 "Field - value" 格式
        r'{field}\s*-\s*([^\n]+)',
        # 兜底：匹配到行尾
        r'{field}\s*:\s*([^\n]+)',
    ]

    # 所有支持的字段名（用于边界匹配）
    ALL_FIELD_NAMES = ['Type', 'Path', 'Operator', 'Value', 'Original\\s+Value', 'Is\\s+Negative']

    @classmethod
    def extract(cls, block: str, field_name: str, block_idx: int, required: bool = False) -> Optional[str]:
        """
        从文本块中提取指定字段的值

        Args:
            block: 约束文本块
            field_name: 字段名（如 "Type", "Path", "Operator"）
            block_idx: 约束块索引（用于日志）
            required: 是否为必需字段

        Returns:
            提取的字段值，如果未找到则返回None
        """
        # 转义字段名用于正则表达式
        escaped_field = re.escape(field_name)
        all_fields_pattern = '|'.join(cls.ALL_FIELD_NAMES)

        # 尝试所有模式
        for pattern_template in cls.FIELD_PATTERNS:
            pattern = pattern_template.format(
                field=escaped_field,
                all_fields=all_fields_pattern
            )
            match = re.search(pattern, block, re.IGNORECASE | re.MULTILINE | re.DOTALL)
            if match:
                value = match.group(1).strip()
                # 清理值：移除末尾可能存在的字段名残留
                value = re.sub(
                    r'\s+(Type|Path|Operator|Value|Original\s+Value|Is\s+Negative)\s*:?\s*$',
                    '',
                    value,
                    flags=re.IGNORECASE
                )
                return value.strip()

        if required:
            _logger.warning(f"Constraint {block_idx}: Missing required field '{field_name}'")
        return None


# ============================================================================
# 辅助类 - 约束块分割器
# ============================================================================

class ConstraintBlockSplitter:
    """约束块分割器 - 负责将约束文本分割成多个独立的约束块"""

    @staticmethod
    def split(constraints_text: str) -> List[str]:
        """
        将约束文本分割成多个独立的约束块

        支持多种格式：
        1. "Constraint N:" 格式
        2. 空行分割
        3. 以 "-" 开头的列表格式

        Args:
            constraints_text: 完整的约束文本

        Returns:
            约束块列表
        """
        # 策略1: 按 "Constraint N:" 分割
        blocks = re.split(r'Constraint\s+\d+\s*:', constraints_text, flags=re.IGNORECASE)

        # 策略2: 如果分割失败，尝试按空行分割
        if len(blocks) == 1 and blocks[0] == constraints_text:
            blocks = re.split(r'\n\s*\n', constraints_text)

        # 策略3: 如果还是只有一个块，尝试按 "- Type:" 或 "- Path:" 分割
        if len(blocks) == 1 and blocks[0] == constraints_text:
            blocks = ConstraintBlockSplitter._split_by_field_markers(constraints_text)

        # 过滤空块
        return [block.strip() for block in blocks if block.strip()]

    @staticmethod
    def _split_by_field_markers(text: str) -> List[str]:
        """按字段标记分割（查找所有以 "-" 开头的 Type/Path 行）"""
        lines = text.split('\n')
        blocks = []
        current_block = []

        for line in lines:
            # 检查是否是新约束的开始（以 "- Type:" 或 "- Path:" 开头）
            if re.match(r'^\s*-\s*(Type|Path)\s*[:|-]', line, re.IGNORECASE) and current_block:
                blocks.append('\n'.join(current_block))
                current_block = [line]
            else:
                current_block.append(line)

        if current_block:
            blocks.append('\n'.join(current_block))

        return blocks


# ============================================================================
# 辅助类 - 约束块解析器
# ============================================================================

class ConstraintBlockParser:
    """约束块解析器 - 负责将单个约束块解析为ExtraConstraint对象"""

    def __init__(self, source_type: str):
        """
        Args:
            source_type: 约束来源类型（buggy/fixed/fp）
        """
        self.source_type = source_type

    def parse(self, block: str, block_idx: int) -> Optional[ExtraConstraint]:
        """
        解析单个约束块

        Args:
            block: 约束文本块
            block_idx: 约束块索引（用于日志）

        Returns:
            解析后的约束对象，如果解析失败则返回None
        """
        # 1. 提取并验证 Type 字段
        constraint_type = self._parse_type(block, block_idx)
        if constraint_type is None:
            return None

        # 2. 提取并验证 Path 字段
        constraint_path = self._parse_path(block, block_idx)
        if constraint_path is None:
            return None

        # 3. 根据类型提取 Operator 和 Value
        operator, value, original_value = self._parse_value_fields(
            block, block_idx, constraint_type
        )
        if constraint_type in [ConstraintType.ADD, ConstraintType.EDIT]:
            if operator is None or value is None:
                return None

        # 4. 提取 Is Negative 字段
        is_negative = self._parse_is_negative(block, block_idx, constraint_type)

        # 5. 创建约束对象
        try:
            constraint = ExtraConstraint(
                constraint_path=constraint_path,
                operator=operator or "",
                value=value or "",
                constraint_type=constraint_type,
                is_negative=is_negative,
                source_file=self.source_type,
                original_value=original_value
            )
            _logger.debug(
                f"Successfully parsed constraint {block_idx}: "
                f"type={constraint_type.value}, path={constraint_path}"
            )
            return constraint
        except Exception as e:
            _logger.error(f"Constraint {block_idx}: Failed to create constraint object: {e}")
            return None

    def _parse_type(self, block: str, block_idx: int) -> Optional[ConstraintType]:
        """解析 Type 字段"""
        type_str = FieldExtractor.extract(block, "Type", block_idx, required=True)
        if not type_str:
            _logger.warning(f"Constraint {block_idx}: Missing Type field, skipping")
            return None

        type_str_lower = type_str.lower().strip()
        if type_str_lower in ['add', 'a']:
            return ConstraintType.ADD
        elif type_str_lower in ['edit', 'e', 'modify', 'update']:
            return ConstraintType.EDIT
        elif type_str_lower in ['del', 'delete', 'remove', 'd']:
            return ConstraintType.DEL
        else:
            _logger.warning(
                f"Constraint {block_idx}: Invalid Type '{type_str}', "
                f"expected 'add', 'edit', or 'del'. Defaulting to 'add'"
            )
            return ConstraintType.ADD

    def _parse_path(self, block: str, block_idx: int) -> Optional[str]:
        """解析并验证 Path 字段"""
        path_str = FieldExtractor.extract(block, "Path", block_idx, required=True)
        if not path_str:
            return None

        constraint_path = path_str.strip()

        # 验证和清理路径格式
        # 路径格式可以是:
        # - "nodeAlias" (对节点本身的约束，如类型检查)
        # - "nodeAlias.attribute" (对属性的约束)
        # - "nodeAlias.attribute.subattribute" (对嵌套属性的约束)
        if constraint_path.endswith('.'):
            _logger.warning(
                f"Constraint {block_idx}: Path ends with '.', "
                f"removing trailing dot: '{constraint_path}'"
            )
            constraint_path = constraint_path.rstrip('.')

        return constraint_path

    def _parse_value_fields(
        self,
        block: str,
        block_idx: int,
        constraint_type: ConstraintType
    ) -> tuple[Optional[str], Optional[str], Optional[str]]:
        """
        解析 Operator、Value 和 Original Value 字段

        Returns:
            (operator, value, original_value) 元组
        """
        operator = None
        value = None
        original_value = None

        if constraint_type in [ConstraintType.ADD, ConstraintType.EDIT]:
            # ADD 和 EDIT 需要 Operator 和 Value
            operator = FieldExtractor.extract(block, "Operator", block_idx, required=True)
            if not operator:
                _logger.warning(
                    f"Constraint {block_idx}: Missing Operator field "
                    f"for {constraint_type.value} type"
                )
                return None, None, None

            value = FieldExtractor.extract(block, "Value", block_idx, required=True)
            if not value:
                _logger.warning(
                    f"Constraint {block_idx}: Missing Value field "
                    f"for {constraint_type.value} type"
                )
                return None, None, None

            # 清理值（处理多行和子查询）
            value = self._clean_value(value)

            # EDIT 类型可能包含 Original Value
            if constraint_type == ConstraintType.EDIT:
                original_value = FieldExtractor.extract(
                    block, "Original Value", block_idx, required=False
                )
                if original_value:
                    original_value = original_value.strip()

        return operator, value, original_value

    @staticmethod
    def _clean_value(value: str) -> str:
        """清理值字符串（处理多行和空格）"""
        if ';' in value:
            # 可能是子查询，保留基本格式但清理多余空格
            return re.sub(r'\n\s+', ' ', value)
        else:
            # 单行值，合并空格
            return re.sub(r'\s+', ' ', value)

    def _parse_is_negative(
        self,
        block: str,
        block_idx: int,
        constraint_type: ConstraintType
    ) -> bool:
        """
        解析 Is Negative 字段

        Returns:
            True if constraint should be negated (wrapped in not(...))
            False otherwise (default)
        """
        negative_str = FieldExtractor.extract(block, "Is Negative", block_idx, required=False)

        if negative_str:
            negative_str_lower = negative_str.lower().strip()
            return negative_str_lower in ['yes', 'true', 'y', '1']
        else:
            # 默认为 False - 大部分约束应该是肯定性的
            # 不再根据 source_type 推断，因为这个假设是错误的：
            # - Scenario 1 (FP缺少特征): 需要添加肯定约束来检测该特征
            # - Scenario 2 (Buggy有特征): 需要添加肯定约束来检测该特征
            if constraint_type != ConstraintType.DEL:
                _logger.debug(
                    f"Constraint {block_idx}: Missing Is Negative field, defaulting to 'no'"
                )
            return False


# ============================================================================
# 辅助类 - 约束响应解析器
# ============================================================================

class ConstraintResponseParser:
    """约束响应解析器 - 主解析器类，协调整个解析流程"""

    # 匹配 [CONSTRAINTS]...[/CONSTRAINTS] 块的正则表达式
    CONSTRAINTS_PATTERN = re.compile(
        r'\[CONSTRAINTS\](.*?)\[/CONSTRAINTS\]',
        re.DOTALL | re.IGNORECASE
    )

    def __init__(self, source_type: str):
        """
        Args:
            source_type: 约束来源类型（buggy/fixed/fp）
        """
        self.source_type = source_type
        self.block_parser = ConstraintBlockParser(source_type)

    def parse(self, response: str) -> List[ExtraConstraint]:
        """
        从LLM响应中解析约束列表

        Args:
            response: LLM的完整响应文本

        Returns:
            解析出的约束对象列表
        """
        # 1. 提取 [CONSTRAINTS] 块
        constraints_text = self._extract_constraints_block(response)
        if constraints_text is None:
            return []

        # 2. 分割成多个约束块
        blocks = ConstraintBlockSplitter.split(constraints_text)
        if not blocks:
            _logger.info("Empty constraints block found")
            return []

        # 3. 解析每个约束块
        constraints = []
        for block_idx, block in enumerate(blocks, 1):
            constraint = self.block_parser.parse(block, block_idx)
            if constraint:
                constraints.append(constraint)

        if not constraints:
            _logger.warning("No valid constraints extracted from response")
            return []

        return constraints

    def _extract_constraints_block(self, response: str) -> Optional[str]:
        """提取 [CONSTRAINTS]...[/CONSTRAINTS] 块"""
        match = self.CONSTRAINTS_PATTERN.search(response)
        if not match:
            _logger.warning("No [CONSTRAINTS] block found in response")
            return None

        constraints_text = match.group(1).strip()
        if not constraints_text:
            return None

        return constraints_text


# ============================================================================
# 主State类
# ============================================================================

class ExtractConstraintState(PromptState):
    """Step3: 提取额外约束"""

    # 用于验证响应格式的正则表达式
    CONSTRAINTS_PATTERN = re.compile(
        r'\[CONSTRAINTS\](.*?)\[/CONSTRAINTS\]',
        re.DOTALL | re.IGNORECASE
    )

    def check_valid(self, response: str) -> bool:
        """验证响应是否包含有效的约束块"""
        match = self.CONSTRAINTS_PATTERN.search(response)
        return bool(match)

    @retry_times(retries=5)
    @valid_with(check_valid)
    def invoke_validate_retry(self, messages: list) -> str:
        """调用LLM并验证响应格式，失败时自动重试"""
        return self.refiner.llm.invoke(messages)

    def parse_constraints(self, response: str, source_type: str) -> List[ExtraConstraint]:
        """
        解析约束文本，提取ExtraConstraint对象

        Args:
            response: LLM的完整响应文本
            source_type: 约束来源类型（buggy/fixed/fp）

        Returns:
            解析出的约束对象列表
        """
        parser = ConstraintResponseParser(source_type)
        return parser.parse(response)

    def accept(self):
        """执行约束提取"""
        # Step3: 获取累积历史（包含Step1和Step2的完整对话）
        base_messages = self.refiner.context.get_accumulated_history(
            up_to_step=RefineStep.EXTRACT_CONSTRAINT
        )

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
            prompt = get_extract_constraint_prompt(
                self.refiner.input_data.dsl_code,
                source_code,
                source_type
            )

            # 每次调用都基于累积历史，并添加当前请求
            current_messages = base_messages.copy()
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

        # 保存提取的约束并转换到下一个状态
        if all_constraints:
            self.refiner.context.extracted_constraints = all_constraints
            from app.refine.states.validate_constraint_state import ValidateConstraintState
            self.refiner.prompt_state = ValidateConstraintState(self.refiner)
        else:
            _logger.warning("No constraints extracted, proceeding to construct DSL with original")
            from app.refine.states.construct_dsl_state import ConstructDSLState
            self.refiner.prompt_state = ConstructDSLState(self.refiner)
