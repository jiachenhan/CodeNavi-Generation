import json
import random
from pathlib import Path
from typing import Dict, List, Generator, Union

import utils
from app.abs.mark2.history import GlobalHistories, MessageList
from app.abs.mark2.prompt_state import InitialState, ExitState
from app.communication import PatternInput
from interface.java.run_java_api import java_extract_pattern, java_llm_abstract, java_generate_query
from interface.llm.llm_api import LLMAPI
from interface.llm.llm_openai import LLMOpenAI
from utils.config import LoggerConfig, set_config

_logger = LoggerConfig.get_logger(__name__)


class Analyzer:
    def __init__(self,
                 llm: LLMAPI,
                 pattern_input: PatternInput,
                 store_path: Path,
                 ori_path,
                 retries: int = 5):
        self.llm = llm
        self.pattern_input = pattern_input
        self.retries = retries
        self.store_path = store_path
        self.ori_path = ori_path
        self.prompt_state = InitialState(self)
        self.global_history = GlobalHistories()

        self.important_lines = []
        self.key_elements = {}

        self.considered_elements = set()

    def analysis(self):
        while not isinstance(self.prompt_state, ExitState):
            self.prompt_state.accept()

    def append_store_history(self, part_history: Dict[str, MessageList]):
        if self.store_path.exists():
            with open(self.store_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            data["histories"].update(part_history)
            with open(self.store_path, 'w', encoding='utf-8') as f:
                json.dump(data, f, indent=4, ensure_ascii=False)
        else:
            _logger.info(f"create {self.store_path} to store history")
            self.store_path.parent.mkdir(parents=True, exist_ok=True)
            # 若文件不存在，则创建新文件并写入初始列表
            with open(self.store_path, 'w', encoding='utf-8') as f:
                data = {
                    "histories": part_history,
                    "roughly_line": [],
                    "considered_elements": [],
                    "considered_attrs": {"exprType": [],
                                         "Name": []},
                    "regex": {},
                    "insert_elements": {},
                    "move_elements": {}
                }
                json.dump(data, f, indent=4, ensure_ascii=False)

    def append_store_info(self, key: str, info: Union[List, Dict]):
        if self.store_path.exists():
            with open(self.store_path, 'r', encoding='utf-8') as f:
                data = json.load(f)
            data[key] = info
            with open(self.store_path, 'w', encoding='utf-8') as f:
                json.dump(data, f, indent=4, ensure_ascii=False)


if __name__ == "__main__":
    config = set_config("yunwu")
    jar_path = config.get("jar_path")
    model_name = config.get("openai").get("model")

    llm = LLMOpenAI(base_url=config.get("openai").get("base_url"),
                    api_key=config.get("openai").get("api_keys")[0],
                    model_name=model_name)

    dataset_name = "ql"
    dataset_path = Path("E:/dataset/Navi/DEFs") / dataset_name

    pattern_path = utils.config.get_pattern_base_path() / model_name / dataset_name
    pattern_info_path = utils.config.get_pattern_info_base_path() / model_name / dataset_name
    dsl_path = utils.config.get_dsl_base_path() / model_name / dataset_name

    def get_random_code_pair(_path: Path) -> Generator[Path, None, None]:
        for _checker in _path.iterdir():
            if not _checker.is_dir():
                continue
            for group in _checker.iterdir():
                if not group.is_dir():
                    continue
                _case_name = random.choice([d for d in group.iterdir() if d.is_dir()])
                case_path = group / _case_name
                yield case_path

    def process_single_case(
            llm: LLMOpenAI,
            jar: str,
            case: Path,
            pattern_path: Path,
            dsl_path: Path,
            pattern_info_path: Path
    ):
        extract_pattern(jar, case, pattern_path, pattern_info_path)
        abstract_pattern(llm, jar, case, pattern_path, pattern_info_path)
        generate_query(jar, case, pattern_path, dsl_path)


    def extract_pattern(_jar: str, _case_path: Path, _pattern_path: Path, _pattern_info_path: Path):
        checker_name = _case_path.parent.parent.stem
        group_name = _case_path.parent.stem
        pattern_ori_path = _pattern_path / "ori" / checker_name / group_name / f"{_case_path.stem}.ser"
        pattern_info_input_path = _pattern_info_path / "input" / checker_name / group_name / f"{_case_path.stem}.json"
        java_extract_pattern(30, _case_path, pattern_ori_path, pattern_info_input_path, _jar)

    def abstract_pattern(
            _llm: LLMOpenAI,
            _jar: str,
            _case_path: Path,
            _pattern_path: Path,
            _pattern_info_path: Path
    ):
        checker_name = _case_path.parent.parent.stem
        group_name = _case_path.parent.stem
        case_info = json.load(open(_case_path / "info.json", 'r'))["may_be_fixed_violations"].strip()

        pattern_ori_path = _pattern_path / "ori" / checker_name / group_name / f"{_case_path.stem}.ser"
        pattern_abs_path = _pattern_path / "abs" / checker_name / group_name / f"{_case_path.stem}.ser"
        pattern_info_input_path = _pattern_info_path / "input" / checker_name / group_name / f"{_case_path.stem}.json"
        pattern_info_output_path = _pattern_info_path / "output" / checker_name / group_name / f"{_case_path.stem}.json"

        pattern_input = PatternInput.from_file(pattern_info_input_path)
        pattern_input.set_error_info(case_info)

        analyzer = Analyzer(_llm, pattern_input, pattern_info_output_path)
        analyzer.analysis()

        java_llm_abstract(30, pattern_ori_path, pattern_info_output_path, pattern_abs_path, _jar)

    def generate_query(_jar: str, _case_path: Path, _pattern_path: Path, _dsl_path: Path):
        checker_name = _case_path.parent.parent.stem
        group_name = _case_path.parent.stem
        pattern_abs_path = _pattern_path / "abs" / checker_name / group_name / f"{_case_path.stem}.ser"
        dsl_output_path = _dsl_path / checker_name / group_name / f"{_case_path.stem}.kirin"

        java_generate_query(30, pattern_abs_path, dsl_output_path, _jar)


    cases = get_random_code_pair(dataset_path)
    for case in cases:
        _logger.info(f"case path: {case}")
        checker_name = case.parent.parent.stem
        group_name = case.parent.stem
        dsl_group_path = dsl_path / checker_name / group_name

        process_single_case(
            llm,
            jar_path,
            case,
            pattern_path,
            dsl_path,
            pattern_info_path
        )
