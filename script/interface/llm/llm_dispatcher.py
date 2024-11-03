import threading
from concurrent.futures import ThreadPoolExecutor, as_completed
from typing import List, Generator

from interface.llm.cost_manager import CostManager
from interface.llm.llm_openai import LLMOpenAI
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


class LLMDispatcher:
    def __init__(self,
                 llm_servers: List[LLMOpenAI]):
        self.llm_servers = llm_servers
        self.available_llms = []  # 可用的LLM列表
        self.condition = threading.Condition()  # 用于管理LLM的条件变量
        self._initialize_llms()  # 初始化可用LLM
        self.cost_manager = CostManager(model_name="ALL")

    # 初始化可用LLM实例
    def _initialize_llms(self):
        with self.condition:
            for llm in self.llm_servers:
                self.available_llms.append(llm)
            self.condition.notify_all()  # 通知所有线程有LLM可用

    def process_task(self, task_func, *args):
        with self.condition:
            # 等待可用的 LLM
            while not self.available_llms:
                self.condition.wait()  # 阻塞，直到有LLM可用

            # 获取一个可用的LLM实例
            llm_server = self.available_llms.pop(0)

        # 执行任务
        try:
            _logger.info(f"Processing task by LLM {llm_server}")
            result = task_func(llm_server, *args)
            self.cost_manager.show_cost()

        finally:
            self.cost_manager.update_all_cost([llm_server.cost_manager for llm_server in self.llm_servers])
            with self.condition:
                # 任务完成，释放LLM，并重新加入可用列表
                self.available_llms.append(llm_server)
                self.condition.notify()  # 通知其他等待的线程

    @staticmethod
    def print_running_threads(_executor):
        running_threads = len([thread for thread in _executor._threads if thread.is_alive()])
        print(f"Running threads in the pool: {running_threads}")

    def submit_tasks(self, _task_generator: Generator):
        """
        提交多个任务给线程池并发处理。
        :param _task_generator: 任务生成器
        """
        _max_workers = len(self.llm_servers)  # 同时工作的最大任务数
        _futures = []
        with ThreadPoolExecutor(max_workers=_max_workers) as executor:
            for task in _task_generator:
                future = executor.submit(self.process_task, task['func'], *task['args'])
                _futures.append(future)

            # 最后确保所有任务都完成
            for future in _futures:
                future.result()  # 确保所有任务完成
