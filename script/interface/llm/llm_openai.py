import os
import threading
import time

import openai
from openai import OpenAI

from interface.llm.cost_manager import CostManager
from interface.llm.llm_api import LLMAPI
from utils.common import retry_times
from utils.config import set_config, LoggerConfig


_logger = LoggerConfig.get_logger(__name__)


class LLMOpenAI(LLMAPI):
    def __init__(self,
                 base_url,
                 api_key,
                 model_name
                 ):
        super().__init__(base_url, api_key)
        self.model_name = model_name
        self.client = OpenAI(
            base_url=self.base_url,
            api_key=self.api_key
        )
        self.cost_manager = CostManager(model_name=self.model_name)

    def invoke(self, _messages) -> str:
        response = None
        while response is None:
            try:
                thread_id = threading.current_thread().ident
                _logger.info(f"Thread (ID: {thread_id}) is about to perform LLM request")
                response = self.client.chat.completions.create(
                    model=self.model_name,
                    messages=_messages,
                    stream=False
                )
                # raw_response = self.client.chat.completions.with_raw_response.create(
                #     model=self.model_name,
                #     messages=_messages,
                #     stream=False
                # )
                # response = raw_response.parse()
                # # 目前没有并发限制（但是实际上算力有限
                # rate_limit_remaining = raw_response.headers.get("x-ratelimit-remaining")
                # if rate_limit_remaining == 0:
                #     _logger.warn(f"Rate limit remaining: {rate_limit_remaining}")
                #     raise Exception("Rate limit exceeded")
            except openai.BadRequestError as e:
                _logger.error(f"OpenAI API error: {e}")
                if "Request timed out" in str(e):
                    time.sleep(30)
                    continue
                else:
                    raise
            except Exception as e:
                import traceback
                traceback.print_exc()
                _logger.error(f"Other error: {e}")
                time.sleep(30)

        if hasattr(response.usage, "prompt_cache_hit_tokens"):
            self.cost_manager.update_cost(
                response.usage.prompt_cache_hit_tokens,
                response.usage.prompt_cache_miss_tokens,
                response.usage.completion_tokens
            )
        else:
            self.cost_manager.update_cost(
                response.usage.prompt_tokens,
                response.usage.completion_tokens
            )
        return response.choices[0].message.content


if __name__ == "__main__":
    set_config("ppinfra")
    print(f"base_url: {os.environ.get('OPENAI_BASE_URL')}")
    deepseek = LLMOpenAI(base_url=os.environ.get("OPENAI_BASE_URL"), api_key=os.environ.get("OPENAI_API_KEY"),
                         model_name=os.environ.get("MODEL_NAME"))
    # codeLlama = LLMOpenAI(base_url="http://localhost:8001/v1", api_key="empty", model_name="CodeLlama")
    messages = [
        {"role": "system", "content": "you are a helpful assistant!"},
        {"role": "user", "content": "hello"}
    ]
    answer = deepseek.invoke(messages)
    print(answer)
    messages = [
        {"role": "system", "content": "you are a helpful assistant!"},
        {"role": "user", "content": "hello"},
        {"role": "assistant", "content": answer},
        {"role": "user", "content": "how are you?"}
    ]
    answer = deepseek.invoke(messages)
    print(answer)
    deepseek.cost_manager.show_cost()
