import os
import threading
import time

from openai import OpenAI

from interface.llm.cost_manager import CostManager
from interface.llm.llm_api import LLMAPI
from utils.config import set_proxy, LoggerConfig

"""
curl https://api.deepseek.com/chat/completions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer sk-92e516aab3d443adb30c6659284163e8" \
  -d '{
        "model": "deepseek-chat",
        "messages": [
          {"role": "system", "content": "You are a helpful assistant."},
          {"role": "user", "content": "Hello!"}
        ],
        "stream": false
      }'
"""

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
                raw_response = self.client.chat.completions.with_raw_response.create(
                    model=self.model_name,
                    messages=_messages,
                    stream=False
                )
                response = raw_response.parse()
                # 目前没有并发限制（但是实际上算力有限
                rate_limit_remaining = raw_response.headers.get("x-ratelimit-remaining")
                if rate_limit_remaining == 0:
                    _logger.warn(f"Rate limit remaining: {rate_limit_remaining}")
                    raise Exception("Rate limit exceeded")
            except Exception as e:
                _logger.error(f"OpenAI API error: {e}")
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
    set_proxy()
    deepseek = LLMOpenAI(base_url="https://api.deepseek.com", api_key="sk-92e516aab3d443adb30c6659284163e8",
                         model_name="deepseek-chat")
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
