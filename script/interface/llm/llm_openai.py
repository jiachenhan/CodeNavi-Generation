from openai import OpenAI

from interface.llm.cost_manager import CostManager
from interface.llm.llm_api import LLMAPI


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
        response = self.client.chat.completions.create(
            model=self.model_name,
            messages=_messages
        )
        self.cost_manager.update_cost(
            response.usage.prompt_tokens,
            response.usage.completion_tokens
        )
        return response.choices[0].message.content


if __name__ == "__main__":
    codeLlama = LLMOpenAI(base_url="http://localhost:8001/v1", api_key="empty", model_name="CodeLlama")
    messages = [
        {"role": "system", "content": "you are a helpful assistant!"},
        {"role": "user", "content": "hello"}
    ]
    print(codeLlama.invoke(messages))
    codeLlama.cost_manager.show_cost()
