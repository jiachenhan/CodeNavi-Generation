import re
from pathlib import Path

from exp.pure_llm.prompt import PURE_LLM_PROMPT_V1
from interface.llm.llm_api import LLMAPI
from utils.common import timeout, retry_times, valid_with
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)

PATTERN = r"```\s*CodeNavi\s*([\s\S]*?)\s*```"

@timeout(20 * 60)
def pure_llm_gen(_llm: LLMAPI,
                 _buggy_path: Path,
                 _fixed_path: Path,
                 _description: str) -> str:
    try:
        with open(_buggy_path, 'r') as f:
            _buggy_code = f.read()

        with open(_fixed_path, 'r') as f:
            _fixed_code = f.read()

        _messages = [
            {"role": "user",
             "content": PURE_LLM_PROMPT_V1.format(description=_description,
                                                  buggy=_buggy_code,
                                                  fixed=_fixed_code)}
        ]
        valid, response = invoke_validate_retry(_llm, _messages)
        if not valid:
            _logger.warning(f"Invalid response: {response}, retry! ")
            return ""
        return get_dsl(response)
    except Exception as e:
        import traceback
        traceback.print_exc()
        _logger.error(f"Error in {_buggy_path}: {e}")
        return ""


def check_valid(response: str) -> bool:
    match = re.search(PATTERN, response)
    return bool(match)


def get_dsl(response: str) -> str:
    match = re.search(PATTERN, response)
    if match:
        return match.group(1)
    else:
        return ""


@retry_times(retries=5)
@valid_with(check_valid)
def invoke_validate_retry(_llm: LLMAPI, messages: list) -> str:
    return _llm.invoke(messages)