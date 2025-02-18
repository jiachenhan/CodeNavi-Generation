import multiprocessing
import threading
import time
from functools import wraps
from typing import Union, Optional

from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)

class Timer:
    def __init__(self):
        self.local = threading.local()

    def __enter__(self):
        self.local.start_time = time.time()
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        self.local.end_time = time.time()
        self.local.elapsed_time = self.local.end_time - self.local.start_time
        print(f"Thread {threading.current_thread().name} - Elapsed time: {self.local.elapsed_time:.2f} seconds")


class TimeoutException(Exception):
    """Custom exception for function timeout"""
    pass

class BusinessException(Exception):
    """业务逻辑异常基类"""
    pass

class InvalidOutputError(BusinessException):
    """输出校验失败"""
    pass

def timeout(seconds):
    """Decorator to enforce a timeout on a function using multiprocessing"""
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            queue = multiprocessing.Queue()

            def target(_queue, *_args, **_kwargs):
                try:
                    result = func(*_args, **_kwargs)
                    _queue.put((True, result))
                except Exception as e:
                    _queue.put((False, e))

            process = multiprocessing.Process(target=target, args=(queue, *args), kwargs=kwargs)
            process.start()
            process.join(seconds)

            if process.is_alive():
                process.terminate()
                process.join()
                raise TimeoutException(f"Function '{func.__name__}' timed out after {seconds} seconds")

            success, value = queue.get()
            if success:
                return value
            else:
                raise value
        return wrapper
    return decorator


def retry_post(max_interval=10):
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            retry_interval = 1 # 重试间隔
            while True:
                try:
                    response = func(*args, **kwargs)
                    return response
                except Exception as e:
                    _logger.error(f"{func.__name__}(Retrying {retry_interval}...): An Exception occurred: {e}")
                    time.sleep(retry_interval)
                    retry_interval = retry_interval * 2 if retry_interval < max_interval else 1
        return wrapper
    return decorator

def retry_times(max_retries=5):
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs) -> (bool, Optional[any]):
            for attempt in range(max_retries):
                try:
                    result = func(*args, **kwargs)
                    return True, result
                except Exception as e:
                    remaining = max_retries - (attempt + 1)
                    _logger.error(f"{func.__name__} failed (attempt {attempt + 1}/{max_retries}), retrying... Error: {e}")
                    if remaining > 0:
                        time.sleep(1)  # 可选的间隔等待，可以按需调整或参数化
            _logger.error(f"{func.__name__} failed after {max_retries} attempts")
            return False, None
        return wrapper
    return decorator