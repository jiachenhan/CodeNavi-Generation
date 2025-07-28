import ast
import copy
import json
import re
from itertools import chain
from typing import Dict, List, Union, TYPE_CHECKING, Generator, Callable


from app.abs.llm_genpat_4_round.prompts import ROUGH_SELECT_LINES_PROMPT, TASK_DESCRIPTION_PROMPT, IDENTIFY_ELEMENTS_PROMPT
from app.basic_modification_analysis import background_analysis
from utils.common import retry_times, valid_with
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


class PromptState:
    def __init__(self, analyzer):
        self.analyzer = analyzer

    def accept(self):
        raise NotImplementedError

    def exit(self):
        pass


class InitialState(PromptState):
    def accept(self):
        self.analyzer.prompt_state = BackGroundState(self.analyzer)


class ExitState(PromptState):
    def accept(self):
        pass


class BackGroundState(PromptState):
    def accept(self):
        background_history = background_analysis(self.analyzer.llm, self.analyzer.pattern_input)
        self.analyzer.global_history.background_history = background_history
        self.exit()
        self.analyzer.prompt_state = TaskState(self.analyzer)

    def exit(self):
        background_history = self.analyzer.global_history.background_history
        self.analyzer.append_store_history({"background": background_history})
        pass


class TaskState(PromptState):
    def accept(self):
        _background_messages_copy = copy.deepcopy(self.analyzer.global_history.background_history)
        task_prompt = [{"role": "user", "content": TASK_DESCRIPTION_PROMPT}]
        _background_messages_copy.extend(task_prompt)
        _background_response2 = self.analyzer.llm.invoke(_background_messages_copy)
        task_prompt.append({"role": "assistant", "content": _background_response2})
        self.analyzer.global_history.task_history = task_prompt

        self.exit()
        self.analyzer.prompt_state = AttentionLineState(self.analyzer)

    def exit(self):
        task_history = self.analyzer.global_history.task_history
        self.analyzer.append_store_history({"task": task_history})
        pass


class AttentionLineState(PromptState):
    """This state allows the model to roughly select important rows"""
    pattern = re.compile(r"""
        # 匹配固定起始标记[critical lines]
        \[critical\s*lines]  
        \s*                  # 允许起始标记后的任意空白（包括换行）
        \|\|\|               # 匹配第一个分隔符
        \s*
        # 捕获关键行号列表部分
        (                    # 开始捕获组group(1)
          \[\s*(?:\d+(?:\s*,\s*\d+)*)?\s*]
        )                    # 结束捕获组
        \s*                  # 允许列表后的空白
        \|\|\|               # 匹配第二个分隔符
        [\s\S]*              # 匹配后续所有内容（分析部分）
    """, re.VERBOSE | re.IGNORECASE)

    def check_valid(self, response: str) -> bool:
        match = re.search(self.pattern, response)
        return bool(match)

    def get_attention_lines(self, response: str) -> list:
        match = re.search(self.pattern, response)
        try:
            return ast.literal_eval(match.group(1))  # 直接返回列表
        except SyntaxError:
            _logger.error(f"can't trans to list: {match.group(1)}")
            return []

    @retry_times(retries=5)
    @valid_with(check_valid)
    def invoke_validate_retry(self, messages: list) -> str:
        return self.analyzer.llm.invoke(messages)

    def accept(self):
        _background_messages_copy = copy.deepcopy(self.analyzer.global_history.background_history)
        _select_lines_prompt = [{"role": "user", "content": ROUGH_SELECT_LINES_PROMPT}]
        _background_messages_copy.extend(_select_lines_prompt)
        valid, response = self.invoke_validate_retry(_background_messages_copy)
        if valid:
            _select_lines_prompt.append({"role": "assistant", "content": response})
            self.analyzer.global_history.attention_line_history = _select_lines_prompt
            attention_lines = self.get_attention_lines(response)
            self.analyzer.important_lines = attention_lines

        self.exit()
        self.analyzer.prompt_state = IdentifyState(self.analyzer)
        return

    def exit(self):
        attention_line_history = self.analyzer.global_history.attention_line_history
        self.analyzer.append_store_history({"attention_line": attention_line_history})
        self.analyzer.append_store_info("roughly_line", self.analyzer.important_lines)
        pass


class IdentifyState(PromptState):
    @staticmethod
    def check_valid(response) -> bool:
        result = IdentifyState.extract_critical_elements(response)
        return isinstance(result, list) and result

    @retry_times(retries=5)
    @valid_with(check_valid)
    def invoke_validate_retry(self, messages: list) -> str:
        return self.analyzer.llm.invoke(messages)

    def accept(self):
        _messages_copy = copy.deepcopy(list(chain(self.analyzer.global_history.background_history,
                                                  self.analyzer.global_history.task_history)))

        # 读取ori_path指向的JSON文件内容
        with open(self.analyzer.ori_path, 'r', encoding='utf-8') as f:
            ori_data = json.load(f)

        json_str = json.dumps(ori_data, indent=2, ensure_ascii=False)
        escaped_json = json_str.replace("{", "{{").replace("}", "}}")

        # 将JSON内容转换为字符串并格式化到提示中
        formatted_prompt = IDENTIFY_ELEMENTS_PROMPT.format(
            Genpat_Json_info=escaped_json
        )

        _identify_elements_prompt2 = [{"role": "user", "content": formatted_prompt}]
        _messages_copy.extend(_identify_elements_prompt2)
        _logger.info("prompt is:\n"+formatted_prompt)

        valid, response = self.invoke_validate_retry(_messages_copy)
        if valid:
            _identify_elements_prompt2.append({"role": "assistant", "content": response})
            self.analyzer.global_history.identify_elements_history = _identify_elements_prompt2
            key_elements = IdentifyState.extract_critical_elements(response)
            self.analyzer.key_elements = key_elements

        self.exit()
        self.analyzer.prompt_state = SelectState(self.analyzer)
        return

    def exit(self):
        identify_elements_history = self.analyzer.global_history.identify_elements_history
        self.analyzer.append_store_history({"identify_elements": identify_elements_history})

    @staticmethod
    def extract_critical_elements(llm_output: str) -> Union[List[Dict], str]:
        """
        从LLM输出中提取关键元素

        返回格式:
         成功时: List[Dict] 元素列表
         失败时: str 错误信息
        """
        # 1. 提取关键元素块
        element_block = IdentifyState.extract_element_block(llm_output)
        if not element_block:
            return "错误：未找到关键元素块标记"

        # 2. 验证元素块格式并分割为行
        element_lines = IdentifyState.validate_and_split_block(element_block)
        if isinstance(element_lines, str):
            return element_lines  # 返回错误信息

        # 3. 解析每个元素行
        elements = []
        for i, line in enumerate(element_lines, start=1):
            result = IdentifyState.parse_element_line(line)
            if "error" in result.keys():
                # 解析一行出现错误那么忽略这一行
                continue
            elements.append(result)

        return elements

    @staticmethod
    def extract_element_block(text: str) -> Union[str, None]:
        """
        从LLM输出中提取整个关键元素块（包括边界标记之间的内容）
        """
        # 编译正则表达式 - 带详细注释
        block_pattern = re.compile(
            r"""
            \[CRITICAL_ELEMENTS_START]  # 起始标记
            (.*?)                       # 捕获组1：元素块内容（非贪婪匹配）
            \[CRITICAL_ELEMENTS_END]    # 结束标记
            """,
            re.DOTALL | re.VERBOSE  # DOTALL: .匹配换行；VERBOSE: 允许注释和空白
        )

        # 查找匹配
        match = block_pattern.search(text)
        return match.group(1).strip() if match else None

    @staticmethod
    def validate_and_split_block(block_text: str) -> Union[List[str], str]:
        """
        验证元素块格式并分割为行

        验证规则:
            1. 每行必须恰好有三个'@@'分隔符
            2. 行不能为空

        返回:
            有效: 行列表
            无效: 错误信息字符串
        """
        # 按换行分割为行列表
        lines = [line.strip() for line in block_text.split('\n') if line.strip()]

        # 验证每行格式
        for i, line in enumerate(lines, start=1):
            # 检查分隔符数量
            if line.count('@@') != 3:
                return f"格式错误：第{i}行应恰好包含三个'@@'分隔符"

        return lines

    @staticmethod
    def parse_element_line(line: str) -> Dict:
        """
        解析单个元素行

        格式: ||| 元素类型 ||| 元素值 ||| 行号
        """
        # 编译解析正则 - 带详细注释
        element_pattern = re.compile(
            r"""
            ^                           # 行开始
            \s*                         # 可选空白
            @@                          # 第一个分隔符
            \s*                         # 可选空白
            ([^@]+?)                    # 捕获组1: 元素类型 (非@的任何字符)
            \s*                         # 可选空白
            @@                          # 第二个分隔符
            \s*                         # 可选空白
            ([^@]+?)                    # 捕获组2: 元素值 (非@的任何字符)
            \s*                         # 可选空白
            @@                          # 第三个分隔符
            \s*                         # 可选空白
            (\d+?)                      # 捕获组3: 行号（如"5")
            \s*                         # 可选空白
            $                           # 行结束
            """,
            re.VERBOSE
        )

        # 尝试匹配
        match = element_pattern.match(line)
        if not match:
            return {"error": f"无效的元素行格式: {line}"}

        # 提取捕获组
        elem_type = match.group(1).strip()
        elem_value = match.group(2).strip()
        line_info = match.group(3).strip()

        # 解析行号信息
        if '-' in line_info:
            try:
                start_str, end_str = line_info.split('-')
                start_line = int(start_str)
                end_line = int(end_str)
            except ValueError:
                return {"error": f"无效的行范围格式: {line_info}"}
        else:
            try:
                start_line = end_line = int(line_info)
            except ValueError:
                return {"error": f"无效的行号格式: {line_info}"}

        # 返回结果
        return {
            "type": elem_type,
            "value": elem_value,
            "start_line": start_line,
            "end_line": end_line
        }


class SelectState(PromptState):

    def accept(self):
        for element in self.get_all_elements(self.analyzer.pattern_input.tree):
            start_line, end_line = element.get("start_line"), element.get("end_line")
            if not any(start_line - 1 <= num <= end_line + 1 for num in self.analyzer.important_lines):
                # 这个节点不在attention的范围内
                continue

            if SelectState.assess_key_similarity(element,
                                                 self.analyzer.key_elements,
                                                 SelectState.similarity_lcs,
                                                 0.85):
                self.analyzer.considered_elements.add(element.get("id"))
        self.exit()
        self.analyzer.prompt_state = ExitState(self.analyzer)

    def exit(self):
        self.analyzer.append_store_info("considered_elements", list(self.analyzer.considered_elements))
        pass

    @staticmethod
    def longest_common_substring(s1: str, s2: str) -> int:
        """动态规划计算最长公共子串长度"""
        m, n = len(s1), len(s2)
        dp = [[0] * (n + 1) for _ in range(m + 1)]
        max_len = 0
        for i in range(1, m + 1):
            for j in range(1, n + 1):
                if s1[i - 1] == s2[j - 1]:
                    dp[i][j] = dp[i - 1][j - 1] + 1
                    max_len = max(max_len, dp[i][j])
        return max_len

    @staticmethod
    def similarity_lcs(s1: str, s2: str) -> float:
        """计算归一化LCS相似度 (0~1)"""
        lcs_len = SelectState.longest_common_substring(s1.lower(), s2.lower())  # 忽略大小写
        total_len = len(s1) + len(s2)
        return (2 * lcs_len) / total_len if total_len > 0 else 0.0

    @staticmethod
    def assess_key_similarity(element: Dict,
                              llm_key_elements: List[Dict],
                              sim_func: Callable[[str, str], float],
                              threshold: float = 0.7) -> bool:
        """
        这个函数用来评估给定element是不是llm认为关键的代码元素
        :param element: 给定代码元素（id, type, value, start_line, end_line）
        :param llm_key_elements: llm提取关键元素 list（type, value, start_line, end_line）
        :param sim_func: 相似度函数
        :param threshold: 相似度阈值
        :return: 给定element是否保留
        """
        return any(sim_func(element.get("value"), llm_element.get("value")) >= threshold
                   for llm_element in llm_key_elements)

    def get_all_elements(self, node: Dict) -> Generator[Dict, None, None]:
        if not node.get("value"):
            return

        yield {
            "id": node.get("id"),
            "type": node.get("type"),
            "value": node.get("value"),
            "start_line": node.get("startLine"),
            "end_line": node.get("endLine"),
        }

        children = node.get("children", [])
        if not children or node.get("leaf"):
            return

        for child in children:
            yield from self.get_all_elements(child)
