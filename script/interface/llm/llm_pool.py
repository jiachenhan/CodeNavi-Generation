import asyncio
import random
from typing import List, Tuple, Callable, Any

from interface.llm.llm_api import LLMAPI
from interface.llm.llm_openai import LLMOpenAI


class AsyncLLMPool:
    def __init__(self, llm_infos: List[Tuple[str, str, str]]):
        """(url, key, model)"""
        self.clients = [
            self._create_client(url, api_key, model_name)
            for url, api_key, model_name in llm_infos
        ]

    @staticmethod
    def _create_client(url: str, api_key: str, model_name: str) -> LLMAPI:
        return LLMOpenAI(url, api_key, model_name)


    async def async_run(self, func: Callable, *args, **kwargs) -> Any:
        """
        通用异步执行入口
        :param func: 要执行的LLM调用函数（如llm_abstract）
        :return: 函数执行结果
        """
        client = random.choice(self.clients)  # 随机负载均衡

        # 将LLM客户端作为第一个参数传入
        return await asyncio.to_thread(
            func,
            client,  # 自动注入client
            *args,
            **kwargs
        )