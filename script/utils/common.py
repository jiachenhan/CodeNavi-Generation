import multiprocessing
import threading
import time
from functools import wraps, reduce
from inspect import signature, Parameter
from typing import Union, Optional, Callable

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


def _global_target(queue, func, args, kwargs):
    """helpful func timeout decorator,
        If closure, a spawn error occurs on Windows platform
    """
    try:
        result = func(*args, **kwargs)
        queue.put((True, result))
    except Exception as e:
        queue.put((False, e))


def timeout(seconds):
    """Decorator to enforce a timeout on a function using multiprocessing"""
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            manager = multiprocessing.Manager()
            queue = manager.Queue()

            process = multiprocessing.Process(
                target=_global_target,
                args=(queue, func, *args),
                kwargs=kwargs,
                daemon=True
            )

            process.start()
            process.join(seconds)

            if process.is_alive():
                process.terminate()
                process.join()
                raise TimeoutException(f"Function '{func.__name__}' timed out after {seconds} seconds")
            else:
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
                except InvalidOutputError as e:
                    _logger.error(f"{func.__name__}(Retrying {retry_interval}...): InvalidOutputError occurred: {e}")
                    time.sleep(retry_interval)
                    retry_interval = retry_interval * 2 if retry_interval < max_interval else 1
                except Exception as e:
                    _logger.error(f"{func.__name__}(Retrying {retry_interval}...): An Exception occurred: {e}")
                    time.sleep(retry_interval)
                    retry_interval = retry_interval * 2 if retry_interval < max_interval else 1
        return wrapper
    return decorator

def retry_times(retries: Union[int, str] = 5 # 兼容类方法（动态绑定）和普通方法
                ) -> Callable:
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs) -> (bool, Optional[any]):
            # 动态解析重试次数
            if isinstance(retries, str):
                # 从实例中获取重试次数
                context = args[0].__dict__ if args and hasattr(args[0], retries) else globals()
                max_retries = context[retries]
            else:
                max_retries = retries

            for attempt in range(max_retries):
                try:
                    result = func(*args, **kwargs)
                    return True, result
                except InvalidOutputError as e:
                    remaining = max_retries - (attempt + 1)
                    _logger.error(f"{func.__name__} failed (attempt {attempt + 1}/{max_retries}), retrying... Error: {e}")
                    if remaining > 0:
                        time.sleep(1)  # 可选的间隔等待，可以按需调整或参数化
                except Exception as e:
                    remaining = max_retries - (attempt + 1)
                    _logger.error(f"{func.__name__} failed (attempt {attempt + 1}/{max_retries}), retrying... Error: {e}")
                    if remaining > 0:
                        time.sleep(1)  # 可选的间隔等待，可以按需调整或参数化
            _logger.error(f"{func.__name__} failed after {max_retries} attempts")
            return False, None
        return wrapper
    return decorator

def valid_with(validator: Union[Callable[..., bool], str] # 兼容类方法（动态绑定）和普通方法
               ) -> Callable:
    def decorator(func):
        @wraps(func)
        def wrapper(*args, **kwargs):
            result = func(*args, **kwargs)

            # 动态获取验证器
            if isinstance(validator, str):
                # 从实例或模块中获取
                validator_func = getattr(args[0], validator) if args else None
                if not validator_func:
                    raise AttributeError(f"Validator {validator} not found")
            else:
                validator_func = validator

            # 获取验证器函数的参数签名
            sig = signature(validator_func)
            # 构建参数字典
            validator_kwargs = {}

            # 构建可用参数池（优先级从高到低）
            context = {
                'result': result,          # 当前函数返回值
                'response': result,          # 当前函数返回值
                'self': args[0] if args else None,  # 实例对象
            }

            # 智能参数匹配
            for name, param in sig.parameters.items():
                # 1. 特殊类型匹配
                if param.annotation == type(result):
                    validator_kwargs[name] = result
                    continue
                # 2. 上下文直接匹配
                if name in context:
                    validator_kwargs[name] = context[name]
                    continue
                # 3. 默认值处理
                elif param.default != Parameter.empty:
                    continue  # 使用校验函数的默认值
                # 4. 无法匹配的必填参数
                else:
                    raise TypeError(f"校验函数 {validator_func.__name__} 缺少必要参数: {name}")
            if not validator_func(**validator_kwargs):
                raise InvalidOutputError(f"{func.__name__} returned invalid output: {result}")
            return result
        return wrapper
    return decorator

if __name__ == "__main__":
    # 测试代码
    @timeout(2)
    def normal_task():
        return "Success"

    @timeout(2)
    def timeout_task():
        import time
        time.sleep(3)
        return "Never Reached"

    @timeout(2)
    def error_task():
        raise ValueError("Something wrong")

    # 测试正常情况
    print(normal_task())  # 输出: "Success"

    # 测试超时
    try:
        print(timeout_task())
    except TimeoutException as e:
        print(e)  # 输出: "Function 'timeout_task' timed out after 2 seconds"

    # 测试子进程异常
    try:
        error_task()
    except ValueError as e:
        print(e)  # 输出: "Something wrong"