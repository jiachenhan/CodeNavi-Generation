import copy
import json
from enum import Enum
from pathlib import Path
from types import MappingProxyType
from typing import Tuple, Optional

from app.basic_modification_analysis import background_analysis
from app.communication import InputSchema, pretty_print_history
from interface.llm.llm_api import LLMAPI
from interface.llm.llm_openai import LLMOpenAI
from utils.config import get_pattern_info_base_path, LoggerConfig
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
        # both the SimpleName and the QualifiedName
        "Name": "Please evaluate whether the name of the element {element} in line {line} is crucial for the "
                "modification."
                "Please answer with 'yes' or 'no': \n"
                "'Yes': The name is crucial for the modification. \n"
                "'No': The name is not crucial for the modification. \n",
        # expressions type
        "Expression": "Please evaluate whether the expression type {type} must be consistent. "
                      "Please answer with 'yes' or 'no': \n"
                      "'Yes': If the expression type {type} is crucial for modifying logic, "
                      "it cannot be changed.\n"
                      "'No': If the expression type {type} is irrelevant during modification, "
                      "type variation is allowed.\n",
    })

    ELEMENT_PROMPT_TEMPLATE = """
        For {element} in line {line}, please evaluate its impact on this set of code modifications. 
        Determine whether the element is critical during modification and answer with 'yes' or 'no': \n
        'Yes': If the attribute is indispensable in the modification, 
        the condition must be met in order to correctly apply the modification. \n
        'No': If the attribute does not have a decisive impact on the modification, 
        it can be ignored or handled flexibly \n
    """

    @staticmethod
    def get_top_stmts_from_tree(tree: dict) -> list:
        for _ in tree["children"]:
            if _["type"] == "MoBlock":
                return _["children"]

    @staticmethod
    def check_valid_response(response: str):
        return response.strip().lower().startswith(("yes", "no"))

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

    # 属性提问不记录在上下文中
    def attr_analysis(self,
                      _history: list,
                      _element: dict,
                      _attrs: dict,
                      retries: int = 5) -> list:
        _history_clone = copy.deepcopy(_history)
        if not _element.get("isExpr"):
            return _history_clone
        _expr_type = _attrs.get("exprType")
        _attr_prompt = ElementAnalysis._template_prompt_map.get("Expression").format(type=_expr_type)
        _history_clone.append({"role": "user", "content": _attr_prompt})
        for _ in range(retries):
            response = self.llm.invoke(_history_clone)
            if ElementAnalysis.check_valid_response(response):
                if response.strip().lower().startswith("yes"):
                    _history_clone.append({"role": "assistant", "content": response})
                    self.considered_attrs[_element.get("id")] = _expr_type
                return _history_clone
        _history_clone.pop()
        return _history_clone

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
                if response.strip().lower().startswith("yes"):
                    self.considered_elements.add(_element.get("id"))
                    _attrs = self.global_schema.attrs[str(_element.get("id"))]
                    _attr_history = self.attr_analysis(_history_clone, _element, _attrs)
                    self.element_history[_element.get("id")] = (AnalysisState.YES, _attr_history)
                    return AnalysisState.YES, _history_clone
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
        for _stmt in _stmts:
            self.elements_prune_analysis(_background_history, _stmt)


def main():
    code_llama = LLMOpenAI(base_url="http://localhost:8001/v1", api_key="empty", model_name="CodeLlama")
    file_path = get_pattern_info_base_path() / "drjava" / "17" / "0.json"
    global_schema = InputSchema.parse_file(file_path)

    background_history = background_analysis(code_llama, global_schema)

    element_analysis = ElementAnalysis(code_llama, global_schema)
    element_analysis.analysis(background_history)
    for e, result in element_analysis.element_history.items():
        state, history = result
        print(f"Element: {e}")
        pretty_print_history(history)

    view_path = get_pattern_info_base_path() / "drjava" / "17" / "0_element_analysis.json"
    element_analysis.view(view_path)
    code_llama.cost_manager.show_cost()


if __name__ == "__main__":
    with Timer():
        main()
