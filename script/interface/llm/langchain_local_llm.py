from typing import Optional, List, Any, Dict

from langchain_core.callbacks import CallbackManagerForLLMRun
from langchain_core.language_models import LLM
from openai import OpenAI
from pydantic import Field

"""
    langgraph is over-encapsulated, so we give up using it
    this class is deprecated
"""


class LangChainCustomLLM(LLM):
    base_url: str = Field(None, description="The base URL of the OpenAI API.")
    api_key: str = Field(None, description="The API key for the OpenAI API.")
    model_name: str = Field(None, description="The name of the model to use.")

    def __init__(self,
                 base_url: str,
                 api_key: str,
                 model_name: str,
                 *args: Any, **kwargs: Any):
        super().__init__(*args, **kwargs)
        self.base_url = base_url
        self.api_key = api_key
        self.model_name = model_name

    def _call(
            self,
            prompt: str,
            stop: Optional[List[str]] = None,
            run_manager: Optional[CallbackManagerForLLMRun] = None,
            **kwargs: Any,
    ) -> str:
        """Run the LLM on the given input.

        Override this method to implement the LLM logic.

        Args:
            prompt: The prompt to generate from.
            stop: Stop words to use when generating. Model output is cut off at the
                first occurrence of any of the stop substrings.
                If stop tokens are not supported consider raising NotImplementedError.
            run_manager: Callback manager for the run.
            **kwargs: Arbitrary additional keyword arguments. These are usually passed
                to the model provider API call.

        Returns:
            The model output as a string. Actual completions SHOULD NOT include the prompt.
        """
        client = OpenAI(
            base_url=self.base_url,
            api_key=self.api_key
        )

        message = [{"role": "user", "content": prompt}]
        # 创建聊天请求
        response = client.chat.completions.create(
            model=self.model_name,  # 替换为实际模型名称
            messages=message
        )
        return response.choices[0].message.content

    @property
    def _identifying_params(self) -> Dict[str, Any]:
        """Return a dictionary of identifying parameters."""
        return {
            # The model name allows users to specify custom token counting
            # rules in LLM monitoring applications (e.g., in LangSmith users
            # can provide per token pricing for their model and monitor
            # costs for the given LLM.)
            "model_name": "Local-CodeLlama-34B",
        }

    @property
    def _llm_type(self) -> str:
        return f"local_{self.model_name}"


if __name__ == "__main__":
    llm = LangChainCustomLLM(base_url="http://localhost:8001/v1", api_key="empty", model_name="CodeLlama")
    print(llm)
    print(llm.invoke("hello llm!"))
