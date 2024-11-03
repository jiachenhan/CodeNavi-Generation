import copy
import json
import re
from enum import Enum
from pathlib import Path
from types import MappingProxyType
from typing import Tuple, Optional

from app.basic_modification_analysis import background_analysis
from app.communication import InputSchema, pretty_print_history
from interface.llm.llm_api import LLMAPI
from interface.llm.llm_openai import LLMOpenAI
from utils.config import get_pattern_info_base_path, LoggerConfig, set_proxy
from utils.timer import Timer

_logger = LoggerConfig.get_logger(__name__)


class AnalysisState(Enum):
    YES = "yes"
    NO = "no"
    ERROR = "error"

    def __json__(self):
        return self.value  # 返回枚举值

    @staticmethod
    def custom_serializer(obj):
        if hasattr(obj, '__json__'):
            return obj.__json__()
        raise TypeError(f"Object of type {type(obj).__name__} is not JSON serializable")


class ElementAnalysis:
    _template_prompt_map = MappingProxyType({
        # both the SimpleName and the QualifiedName (ask directly in the section that integrates elements part)
        "Name": "Please evaluate whether the name of the element {element} in line {line} is crucial for the "
                "modification. "
                "'Yes': The name is crucial for the modification. \n"
                "'No': The name is not crucial for the modification. \n"
                "Please answer the question according to the following template: \n"
                "[yes/no]: [Cause analysis] \n",
                # "Template: \n"
                # "No: The name getFile in line 6 is not critical "
                # "for the modification invoicing the addition of the try catch block for IOException "
                # "The primary purpose of this block is to ensure thread safety "
                # "when incrementing the `saveBeforeCompileCount` variable.",
        # expressions type
        "Expression": "Please evaluate whether the expression type {type} must be consistent. "
                      "'Yes': If the expression type {type} is crucial for modifying logic, "
                      "it cannot be changed.\n"
                      "'No': If the expression type {type} is irrelevant during modification, "
                      "type variation is allowed.\n"
                      "Please answer the question according to the following template: \n"
                      "[yes/no]: [Cause analysis] \n",
                      # "Template: \n"
                      # "Yes: The expression type `int` is critical, because  it is a conditional variable "
                      # "in a synchronous block that controls thread safety during concurrency",
        # structure information of the element
        "Structure": "Is the structural information of {elementType} crucial for modification? "
                     "Or is it simply because it contains important code elements?"
                     "'Yes': The structure is crucial for the modification. \n"
                     "'No': The structure is not crucial for the modification. \n"
                     "Please answer the question according to the following template: \n"
                     "[yes/no]: [Cause analysis] \n",
                     # "Template: \n"
                     # "No: the for loop structure is not critical, "
                     # "because the action of adding elements to the list itself is important for modification, "
                     # "and the loop only repeats this step",
    })

    ELEMENT_PROMPT_TEMPLATE = """
        For {element} in line {line}, please evaluate code snippet impact on this set of code modifications. 
        Determine whether it contains key code that should be modified, 
        or contains code elements that are related to the modification. \n
        'Yes': If the code snippet contains crucial part in the modification, 
        because it implies semantics or contains modifications themselves. \n
        'No': If the code snippet does not contain crucial part on the modification, 
        it can be ignored or handled flexibly \n
        
        "Please answer the question according to the following template: \n"
        "[yes/no]: [Cause analysis] \n"
    """

    @staticmethod
    def get_top_stmts_from_tree(tree: dict) -> list:
        for _ in tree["children"]:
            if _["type"] == "MoBlock":
                return _["children"]

    @staticmethod
    def check_valid_response(response: str) -> bool:
        # 使用正则表达式删除第一个字母之前的所有符号，并保留整个字符串
        cleaned_response = re.sub(r'^[^a-zA-Z]*([a-zA-Z])', r'\1', response)
        is_valid = cleaned_response.lower().startswith(("yes", "no"))
        if not is_valid:
            _logger.error(f"Retry! Invalid response: {response}")
        return is_valid

    @staticmethod
    def check_true_response(response: str) -> bool:
        cleaned_response = re.sub(r'^[^a-zA-Z]*([a-zA-Z])', r'\1', response)
        return cleaned_response.lower().startswith("yes")

    @staticmethod
    def is_name_element(_element: dict) -> bool:
        return _element.get("type") in ("MoSimpleName", "MoQualifiedName")

    def __init__(self,
                 llm: LLMAPI,
                 _global_schema: InputSchema):
        self.element_history = dict()
        self.considered_elements = set()
        self.considered_attrs = dict()

        self.llm = llm
        self.global_schema = _global_schema

    def view(self, path: Path):
        data = {
            "history": self.element_history,
            "considered_elements": list(self.considered_elements),
            "considered_attrs": self.considered_attrs
        }

        if not path.exists():
            path.parent.mkdir(parents=True, exist_ok=True)
            _logger.info(f"Create file: {path}")

        with open(path, 'w') as f:
            json.dump(data, f, default=AnalysisState.custom_serializer, indent=4)

    def structure_analysis(self,
                           _history: list,
                           _element: dict,
                           retries: int = 5) -> Tuple[AnalysisState, Optional[list]]:
        _history_clone = copy.deepcopy(_history)
        _element_type = _element.get("type")
        _element_prompt = ElementAnalysis._template_prompt_map.get("Structure").format(elementType=_element_type)
        _history_clone.append({"role": "user", "content": _element_prompt})
        _chat_round = [{"role": "user", "content": _element_prompt}]
        for _ in range(retries):
            response = self.llm.invoke(_history_clone)
            if ElementAnalysis.check_valid_response(response):
                if ElementAnalysis.check_true_response(response):
                    _chat_round.append({"role": "assistant", "content": response})
                    return AnalysisState.YES, _chat_round
                else:
                    _chat_round.append({"role": "assistant", "content": response})
                    return AnalysisState.NO, _chat_round
        _history_clone.pop()
        return AnalysisState.ERROR, None

    # 属性提问不记录在上下文中
    def attr_analysis(self,
                      _history: list,
                      _element: dict,
                      _attrs: dict,
                      retries: int = 5) -> Tuple[AnalysisState, Optional[list]]:
        _history_clone = copy.deepcopy(_history)
        if not _element.get("isExpr"):
            return AnalysisState.NO, _history_clone
        _expr_type = _attrs.get("exprType")
        if _expr_type == "<UNKNOWN>":
            return AnalysisState.NO, _history_clone

        _attr_prompt = ElementAnalysis._template_prompt_map.get("Expression").format(type=_expr_type)
        _history_clone.append({"role": "user", "content": _attr_prompt})
        _chat_round = [{"role": "user", "content": _attr_prompt}]
        for _ in range(retries):
            response = self.llm.invoke(_history_clone)
            if ElementAnalysis.check_valid_response(response):
                if ElementAnalysis.check_true_response(response):
                    self.considered_attrs[_element.get("id")] = _expr_type
                    _chat_round.append({"role": "assistant", "content": response})
                    return AnalysisState.YES, _chat_round
                else:
                    _chat_round.append({"role": "assistant", "content": response})
                    return AnalysisState.NO, _chat_round
        _history_clone.pop()
        return AnalysisState.ERROR, None

    def element_analysis(self,
                         _history: list,
                         _element: dict,
                         retries: int = 5) -> Tuple[AnalysisState, Optional[list]]:
        if ElementAnalysis.is_name_element(_element):
            _element_prompt = ElementAnalysis._template_prompt_map.get("Name").format(line=_element.get("startLine"),
                                                                                      element=_element.get("value"))
        else:
            _element_prompt = ElementAnalysis.ELEMENT_PROMPT_TEMPLATE.format(line=_element.get("startLine"),
                                                                             element=_element.get("value"))
        _history_clone = copy.deepcopy(_history)
        _history_clone.append({"role": "user", "content": _element_prompt})
        for _ in range(retries):
            response = self.llm.invoke(_history_clone)
            if ElementAnalysis.check_valid_response(response):
                _history_clone.append({"role": "assistant", "content": response})
                # print(f"history_clone: {_history_clone}")
                if ElementAnalysis.check_true_response(response):
                    _structure_state, _structure_round = self.structure_analysis(_history_clone, _element)
                    # print(f"structure_round: {_structure_round}")
                    if _structure_state == AnalysisState.YES:
                        self.considered_elements.add(_element.get("id"))
                        _attrs = self.global_schema.attrs[str(_element.get("id"))]
                        _attr_state, _attr_round = self.attr_analysis(_history_clone, _element, _attrs)
                        # print(f"attr_round: {_attr_round}")
                        self.element_history[_element.get("id")] = (AnalysisState.YES, _history_clone,
                                                                    _structure_round, _attr_round)
                        return AnalysisState.YES, _history_clone
                    else:
                        self.element_history[_element.get("id")] = (AnalysisState.NO, _history_clone,
                                                                    _structure_round)
                        return AnalysisState.NO, _history_clone
                else:
                    self.element_history[_element.get("id")] = (AnalysisState.NO, _history_clone)
                    return AnalysisState.NO, _history_clone
        return AnalysisState.ERROR, None

    def elements_prune_analysis(self,
                                _parent_history: list,
                                _element: dict) -> None:
        # print(f"Element: {_element['id']}")
        element_analysis_state, element_analysis_result = self.element_analysis(_parent_history, _element)
        if element_analysis_state == AnalysisState.ERROR:
            return
        if not _element.get("leaf"):
            for child in _element.get("children"):
                if element_analysis_state == AnalysisState.YES:
                    self.elements_prune_analysis(element_analysis_result, child)

    def analysis(self,
                 _background_history: list) -> None:
        _stmts = ElementAnalysis.get_top_stmts_from_tree(self.global_schema.tree)
        if not _stmts:
            return
        for _stmt in _stmts:
            self.elements_prune_analysis(_background_history, _stmt)


def main():
    set_proxy()
    code_llama = LLMOpenAI(base_url="https://api.deepseek.com", api_key="sk-92e516aab3d443adb30c6659284163e8",
                           model_name="deepseek-chat")
    file_path = get_pattern_info_base_path() / "input" / "c3_random_1000" / "ant" / "10142" / "1.json"
    global_schema = InputSchema.parse_file(file_path)

    background_history = background_analysis(code_llama, global_schema)

    element_analysis = ElementAnalysis(code_llama, global_schema)
    element_analysis.analysis(background_history)

    view_path = get_pattern_info_base_path() / "input" / "c3_random_1000" / "ant" / "10142" / "1_element_analysis.json"
    element_analysis.view(view_path)
    code_llama.cost_manager.show_cost()


if __name__ == "__main__":
    with Timer():
        main()
