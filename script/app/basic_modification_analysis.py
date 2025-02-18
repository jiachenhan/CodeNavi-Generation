from app.communication import PatternInput, pretty_print_history
from app.general_prompts import BACKGROUND_NO_ERROR_INFO_PROMPT, BACKGROUND_WITH_ERROR_INFO_PROMPT
from interface.llm.llm_api import LLMAPI
from interface.llm.llm_openai import LLMOpenAI
from utils.config import get_pattern_info_base_path


def original_code(input_schema: PatternInput) -> str:
    return "\n".join(input_schema.before_code)


def basic_change_info(input_schema: PatternInput) -> str:
    change_prompt = f"""there are {len(input_schema.diff)} changes in the code, """

    for i, change in enumerate(input_schema.diff):
        index = i + 1
        change_prompt += f"Change {index} is {change['Type']} at line {change['LineStart']}.\n"
        if change["Type"] == "INSERT":
            change_prompt += f"Inserted code is {change['Revised']}.\n"
        elif change["Type"] == "DELETE":
            change_prompt += f"Deleted code is {change['Original']}.\n"
        elif change["Type"] == "CHANGE":
            change_prompt += f"Original code is {change['Original']}.\n"
            change_prompt += f"Revised code is {change['Revised']}.\n"
    return change_prompt


def background_analysis(_llm: LLMAPI, _global_schema: PatternInput) -> list:
    if _global_schema.error_info:
        _background_messages = [
            {"role": "user",
             "content": BACKGROUND_WITH_ERROR_INFO_PROMPT.format(original_code=original_code(_global_schema),
                                                                 change_info=basic_change_info(_global_schema),
                                                                 error_info=_global_schema.error_info)},
        ]
    else:
        _background_messages = [
            {"role": "user",
             "content": BACKGROUND_NO_ERROR_INFO_PROMPT.format(original_code=original_code(_global_schema),
                                                               change_info=basic_change_info(_global_schema))},
        ]

    _background_response1 = _llm.invoke(_background_messages)
    _background_messages.append({"role": "assistant", "content": _background_response1})
    return _background_messages


if __name__ == "__main__":
    codeLlama = LLMOpenAI(base_url="http://localhost:8001/v1", api_key="empty", model_name="CodeLlama")
    file_path = get_pattern_info_base_path() / "drjava" / "17" / "0.json"
    global_schema = PatternInput.parse_file(file_path)

    history = background_analysis(codeLlama, global_schema)
    pretty_print_history(history)
