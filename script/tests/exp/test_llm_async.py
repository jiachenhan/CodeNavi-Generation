"""
测试LLM异步并发功能
"""
import pytest
import asyncio
import time

from exp.thesis.refine.experiment_config import ExperimentConfig


class MockAsyncLLMPool:
    """模拟LLM池用于测试（带并发控制）"""

    def __init__(self, pool_size: int):
        self.pool_size = pool_size
        self.semaphore = asyncio.Semaphore(pool_size)
        self.active_tasks = 0
        self.max_concurrent_reached = 0

    async def execute_task(self, task_id: int, delay: float = 0.5) -> dict:
        """执行单个任务（模拟LLM调用）"""
        async with self.semaphore:
            self.active_tasks += 1
            if self.active_tasks > self.max_concurrent_reached:
                self.max_concurrent_reached = self.active_tasks

            start_time = time.time()
            await asyncio.sleep(delay)
            elapsed = time.time() - start_time

            self.active_tasks -= 1

            return {
                "task_id": task_id,
                "elapsed": elapsed,
                "delay": delay
            }


class TestLLMAsyncConcurrency:
    """测试LLM异步并发"""

    @pytest.fixture
    def small_pool_config(self):
        """创建小池子的配置用于测试"""
        return ExperimentConfig(
            llm_config_tag="yunwu",
            llm_pool_size=2,
            task_timeout=30
        )

    @pytest.mark.asyncio
    async def test_pool_basic_functionality(self, small_pool_config):
        """测试池子基础功能"""
        pool = MockAsyncLLMPool(pool_size=small_pool_config.llm_pool_size)

        assert pool.pool_size == 2
        assert pool.semaphore._value == 2
        assert pool.active_tasks == 0

    @pytest.mark.asyncio
    async def test_concurrent_execution_with_small_pool(self, small_pool_config):
        """测试小池子并发执行（池子大小=2，任务数=4）"""
        pool_size = small_pool_config.llm_pool_size
        pool = MockAsyncLLMPool(pool_size=pool_size)

        task_count = 4
        task_delay = 0.5

        start_time = time.time()
        results = await asyncio.gather(
            *[pool.execute_task(i, task_delay) for i in range(task_count)]
        )
        total_time = time.time() - start_time

        assert len(results) == task_count
        for i, result in enumerate(results):
            assert result["task_id"] == i
            assert result["elapsed"] >= task_delay * 0.9

        assert pool.max_concurrent_reached <= pool_size
        assert pool.max_concurrent_reached == pool_size

        assert total_time >= task_delay * (task_count / pool_size) * 0.9
        assert total_time < task_delay * (task_count / pool_size) * 1.3

    @pytest.mark.asyncio
    async def test_sequential_vs_concurrent_performance(self):
        """对比顺序执行和并发执行的性能"""
        task_count = 6
        task_delay = 0.3

        pool_sequential = MockAsyncLLMPool(pool_size=1)
        start_time = time.time()
        sequential_results = await asyncio.gather(
            *[pool_sequential.execute_task(i, task_delay) for i in range(task_count)]
        )
        sequential_time = time.time() - start_time

        pool_concurrent = MockAsyncLLMPool(pool_size=task_count)
        start_time = time.time()
        concurrent_results = await asyncio.gather(
            *[pool_concurrent.execute_task(i, task_delay) for i in range(task_count)]
        )
        concurrent_time = time.time() - start_time

        assert len(sequential_results) == len(concurrent_results) == task_count
        assert concurrent_time < sequential_time
        assert sequential_time >= task_count * task_delay * 0.9
        assert sequential_time < task_count * task_delay * 1.2
        assert concurrent_time >= task_delay * 0.9
        assert concurrent_time < task_delay * 1.5

    @pytest.mark.asyncio
    async def test_different_pool_sizes(self):
        """测试不同池子大小的性能差异"""
        task_count = 8
        task_delay = 0.2

        async def run_with_pool_size(pool_size: int):
            pool = MockAsyncLLMPool(pool_size=pool_size)
            start_time = time.time()
            await asyncio.gather(
                *[pool.execute_task(i, task_delay) for i in range(task_count)]
            )
            elapsed = time.time() - start_time
            return elapsed, pool.max_concurrent_reached

        time_1, max_1 = await run_with_pool_size(1)
        time_2, max_2 = await run_with_pool_size(2)
        time_4, max_4 = await run_with_pool_size(4)
        time_8, max_8 = await run_with_pool_size(8)

        assert max_1 == 1 and max_2 == 2 and max_4 == 4 and max_8 == 8
        assert time_1 > time_2 > time_4 > time_8
        assert time_1 >= task_count * task_delay * 0.9
        assert time_8 >= task_delay * 0.9


class TestLLMPoolErrorHandling:
    """测试LLM池错误处理"""

    @pytest.mark.asyncio
    async def test_task_timeout_handling(self):
        """测试任务超时处理"""
        config = ExperimentConfig(llm_pool_size=1, task_timeout=1)
        pool = MockAsyncLLMPool(pool_size=config.llm_pool_size)

        with pytest.raises(asyncio.TimeoutError):
            await asyncio.wait_for(
                pool.execute_task(0, delay=2.0),
                timeout=config.task_timeout
            )

    @pytest.mark.asyncio
    async def test_partial_task_failures(self):
        """测试部分任务失败的情况"""
        pool = MockAsyncLLMPool(pool_size=2)

        async def task_with_failure(task_id: int):
            async with pool.semaphore:
                await asyncio.sleep(0.1)
                if task_id == 2:
                    raise ValueError(f"Task {task_id} failed")
                return task_id

        results = await asyncio.gather(
            *[task_with_failure(i) for i in range(5)],
            return_exceptions=True
        )

        assert len(results) == 5
        assert results[0] == 0
        assert isinstance(results[2], ValueError)
        assert results[4] == 4


class TestRealLLMConfig:
    """测试真实LLM配置"""

    def test_get_yunwu_config(self):
        """测试获取yunwu配置"""
        config = ExperimentConfig(llm_config_tag="yunwu")
        llm_config = config.get_llm_config()

        assert llm_config is not None
        assert "openai" in llm_config
        assert len(llm_config["openai"]["api_keys"]) > 0
