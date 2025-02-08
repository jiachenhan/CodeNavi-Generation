from app.communication import PatternInput, pretty_print_history
from app.prompts import BACKGROUND_PROMPT, TASK_DESCRIPTION_PROMPT
from interface.llm.llm_api import LLMAPI
from interface.llm.llm_openai import LLMOpenAI
from utils.config import get_pattern_info_base_path

SYSTEM_PROMPT1 = BACKGROUND_PROMPT

SYSTEM_PROMPT2 = TASK_DESCRIPTION_PROMPT


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
    _background_messages = [
        {"role": "user", "content": SYSTEM_PROMPT1.format(original_code=original_code(_global_schema),
                                                          change_info=basic_change_info(_global_schema))},
    ]
    _background_response1 = _llm.invoke(_background_messages)

    _background_messages.append({"role": "assistant", "content": _background_response1})
    _background_messages.append({"role": "user", "content": SYSTEM_PROMPT2})
    _background_response2 = _llm.invoke(_background_messages)

    _background_messages.append({"role": "assistant", "content": _background_response2})
    # print(f"background_messages: {_background_messages}")
    return _background_messages


if __name__ == "__main__":
    codeLlama = LLMOpenAI(base_url="http://localhost:8001/v1", api_key="empty", model_name="CodeLlama")
    file_path = get_pattern_info_base_path() / "drjava" / "17" / "0.json"
    global_schema = PatternInput.parse_file(file_path)

    history = background_analysis(codeLlama, global_schema)
    pretty_print_history(history)
