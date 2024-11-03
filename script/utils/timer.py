import threading
import time


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
