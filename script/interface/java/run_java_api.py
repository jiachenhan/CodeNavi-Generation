import subprocess
from pathlib import Path
from threading import Timer
from typing import List

import utils.config
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def kill_process(p):
    try:
        p.kill()  # 尝试杀掉进程
        print("Process was terminated due to timeout.")
    except Exception as e:
        print("Error terminating process:", e)


def start_process(cmd: List[str], work_dir: Path, timeout_sec: float) -> str:
    process = subprocess.Popen(cmd,
                               stdout=subprocess.PIPE,
                               stderr=subprocess.PIPE,
                               cwd=str(work_dir),
                               text=True,
                               encoding="utf-8")

    timer = Timer(timeout_sec, kill_process, [process])
    stdout = ""
    try:
        timer.start()  # 启动定时器
        stdout, stderr = process.communicate()  # 等待进程完成
        if process.returncode == 0:
            print(stdout)
        else:
            print("Process exited with errors")
            print(stderr)
    finally:
        timer.cancel()  # 确保定时器取消，避免错误的杀掉进程
    return stdout


def java_genpat_repair(timeout_sec: float,
                       # 使用genpat方法产生补丁
                       pattern_pair_path: Path,
                       buggy_pair_path: Path,
                       patch_path: Path,
                       java_program: str):
    _logger.info(f"Start genpat repair use {pattern_pair_path} for {buggy_pair_path.stem}")
    work_dir = utils.config.get_root_project_path()
    cmd = ["java", "-jar", java_program, "genpat",
           str(pattern_pair_path), str(buggy_pair_path), str(patch_path)]
    start_process(cmd, work_dir, timeout_sec)


def java_gain_oracle(timeout_sec: float,
                     # 解析oracle，写入temp文件
                     oracle_path: Path,
                     method_signature: str,
                     temp_path: Path,
                     java_program: str):
    work_dir = utils.config.get_root_project_path()
    cmd = ["java", "-jar", java_program, "oracle",
           str(oracle_path), str(method_signature), str(temp_path)]
    start_process(cmd, work_dir, timeout_sec)


def java_extract_pattern(timeout_sec: float,
                         # 提取pattern
                         pattern_pair_path: Path,
                         pattern_ser_path: Path,
                         pattern_json_path: Path,
                         java_program: str):
    work_dir = utils.config.get_root_project_path()
    cmd = ["java", "-jar", java_program, "extract",
           str(pattern_pair_path), str(pattern_ser_path), str(pattern_json_path)]
    start_process(cmd, work_dir, timeout_sec)
