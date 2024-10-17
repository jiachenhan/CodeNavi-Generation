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
            _logger.info(f"Processing task by LLM {llm_server.base_url}")
            result = task_func(llm_server, *args)
            with threading.Lock():
                self.cost_manager.update_all_cost([llm_server.cost_manager for llm_server in self.llm_servers])
            self.cost_manager.show_cost()

        finally:
            with threading.Lock():
                self.cost_manager.update_all_cost([llm_server.cost_manager for llm_server in self.llm_servers])

            with self.condition:
                # 任务完成，释放LLM，并重新加入可用列表
                self.available_llms.append(llm_server)
                self.condition.notify()  # 通知其他等待的线程

    def submit_tasks(self, _task_generator: Generator):
        """
        提交多个任务给线程池并发处理。
        :param _task_generator: 任务生成器
        """
        _max_workers = len(self.llm_servers)  # 同时工作的最大任务数
        _futures = []
        with ThreadPoolExecutor(max_workers=_max_workers) as executor:
            # for task in _task_generator:
            #     future = executor.submit(self.process_task, task['func'], *task['args'])
            #     _futures.append(future)

            # 提前提交最多 max_workers 数量的任务
            for _ in range(_max_workers):
                try:
                    task = next(_task_generator)
                    _logger.info(f"提交任务: {task['func']}")
                    future = executor.submit(self.process_task, task['func'], *task['args'])
                    _futures.append(future)
                except StopIteration:
                    break  # 没有更多任务可提交

            # 每当有一个任务完成时，再提交新的任务
            for future in as_completed(_futures):
                _logger.info(f"任务完成: {future}")
                future.result()  # 确保任务完成
                try:
                    # 任务完成后继续提交新任务
                    task = next(_task_generator)
                    _logger.info(f"提交任务: {task['func']}")
                    new_future = executor.submit(self.process_task, task['func'], *task['args'])
                    _futures.append(new_future)
                except StopIteration:
                    continue  # 如果没有更多任务，则继续处理现有的任务

            # 最后确保所有任务都完成
            for future in _futures:
                future.result()  # 确保所有任务完成
