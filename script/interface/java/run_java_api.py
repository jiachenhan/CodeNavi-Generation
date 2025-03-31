import subprocess
from pathlib import Path
from typing import List, Tuple

import utils.config
from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)


def start_process(cmd: List[str], work_dir: Path, timeout_sec: float) -> Tuple[bool, str]:
    process = subprocess.Popen(cmd,
                               stdout=subprocess.PIPE,
                               stderr=subprocess.PIPE,
                               cwd=str(work_dir),
                               text=True,
                               encoding="utf-8",
                               errors="replace")
    timed_out = False  # 新增：超时状态标志
    stdout = ""
    try:
        _logger.info(f"Starting process: {cmd}")
        stdout, stderr = process.communicate(timeout=timeout_sec)  # 直接设置超时参数
        if process.returncode == 0:
            print(stdout)
        else:
            _logger.error("Process exited with errors")
            print(stdout)
            print(stderr)
    except subprocess.TimeoutExpired:
        # 超时处理：标记超时状态并终止进程
        timed_out = True
        process.kill()
        stdout, stderr = process.communicate()  # 清理残留输出
        print(f"Process timed out after {timeout_sec} seconds")
    return timed_out, stdout


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


def java_genpat_detect(timeout_sec: float,
                       # 使用genpat方法检测bug
                       pattern_pair_path: Path,
                       pattern_info_path: Path,
                       repo_path: Path,
                       buggy_info_path: Path,
                       output_path: Path,
                       java_program: str):
    _logger.info(f"Start genpat detect use {pattern_pair_path} for {buggy_info_path.stem}")
    work_dir = utils.config.get_root_project_path()
    cmd = ["java", "-jar", java_program, "genpat_detect",
           str(pattern_pair_path), str(pattern_info_path), str(repo_path), str(buggy_info_path), str(output_path)]
    start_process(cmd, work_dir, timeout_sec)


def java_detect(timeout_sec: float,
                # 使用genpat方法检测bug
                pattern_abs_path: Path,
                repo_path: Path,
                buggy_info_path: Path,
                output_path: Path,
                java_program: str):
    _logger.info(f"Start detect use {pattern_abs_path} for {buggy_info_path.stem}")
    work_dir = utils.config.get_root_project_path()
    cmd = ["java", "-jar", java_program, "detect",
           str(pattern_abs_path), str(repo_path), str(buggy_info_path), str(output_path)]
    start_process(cmd, work_dir, timeout_sec)


def java_abstract(timeout_sec: float,
                  # LLM的抽象结果抽象pattern
                  origin_pattern_path: Path,
                  abstract_info_path: Path,
                  abstract_pattern_path: Path,
                  java_program: str):
    _logger.info(f"Start abstract pattern for {origin_pattern_path.stem}")
    work_dir = utils.config.get_root_project_path()
    cmd = ["java", "-jar", java_program, "abstract",
           str(origin_pattern_path), str(abstract_info_path), str(abstract_pattern_path)]
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
    print(f"cmd: {cmd}")
    start_process(cmd, work_dir, timeout_sec)

def java_generate_query(timeout_sec: float,
                        # generate DSL from abstracted pattern
                        pattern_ser_path: Path,
                        output_query_path: Path,
                        java_program: str):
    work_dir = utils.config.get_root_project_path()
    cmd = ["java", "-jar", java_program, "genquery",
           str(pattern_ser_path), str(output_query_path)]
    start_process(cmd, work_dir, timeout_sec)


def kirin_engine(timeout_sec: float,
                 engine_path: Path,
                 dsl_path: Path,
                 scanned_file_path: Path,
                 output_dir: Path,
                 language: str = "java") -> bool:
    work_dir = utils.config.get_root_project_path()
    cmd = ["java",
           "-Dfile.encoding=utf-8",
           "--add-opens=java.base/java.util=ALL-UNNAMED",
           "--add-opens=java.base/java.lang=ALL-UNNAMED",
           "--add-opens=java.base/java.lang.invoke=ALL-UNNAMED",
           "--add-opens=java.prefs/java.util.prefs=ALL-UNNAMED",
           "--add-opens=java.base/java.nio.charset=ALL-UNNAMED",
           "--add-opens=java.base/java.net=ALL-UNNAMED",
           "--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED",
           "--add-opens=java.base/java.lang.reflect=ALL-UNNAMED",
           "--enable-preview",
           "-cp", str(engine_path), "com.huawei.secbrella.kirin.Main",
           "--plugin",
           "--dir", str(scanned_file_path),
           "--outputFormat", "xml",
           "--output", str(output_dir),
           "--checkerDir", dsl_path,
           "--language", language
           ]

    timeout, sout = start_process(cmd, work_dir, timeout_sec)
    return timeout


def outer_kirin_engine(timeout_sec: float,
                 engine_path: str,
                 dsl_path: Path,
                 scanned_file_path: Path,
                 output_dir: Path,
                 language: str = "java") -> bool:
    work_dir = utils.config.get_root_project_path()
    cmd = ["java",
           "-Dfile.encoding=utf-8",
           "-Djava.io.tmpdir=/data/jiangjiajun/CodeNavi-DSL/tmp",
           "-cp", engine_path, "com.huawei.secbrella.kirin.Main",
           "--plugin",
           "--dir", str(scanned_file_path),
           "--outputFormat", "xml",
           "--output", str(output_dir),
           "--checkerDir", str(dsl_path),
           "--language", language
           ]

    timeout, sout = start_process(cmd, work_dir, timeout_sec)
    return timeout

def kirin_validate(timeout_sec: float,
                   engine_path: Path,
                   dsl_path: Path) -> bool:
    work_dir = utils.config.get_root_project_path()
    cmd = ["java",
           "-Dfile.encoding=utf-8",
           "-cp", str(engine_path), "com.huawei.secbrella.kirin.horn.HornMain",
           "validate",
           str(dsl_path)
           ]

    timeout, sout = start_process(cmd, work_dir, timeout_sec)
    return "invalid" not in sout


def genpat_detect(timeout_sec: float,
                  pattern_before: Path,
                  pattern_after: Path,
                  test_file: Path,
                  jar_path: Path) -> bool:
    work_dir = jar_path.parent

    cmd = ["java",
           "-Dfile.encoding=utf-8",
           "-jar", str(jar_path), "match", str(pattern_before), str(pattern_after), str(test_file)]

    timeout, sout = start_process(cmd, work_dir, timeout_sec)
    if timeout:
        return False
    return "YES" in sout.strip().upper()


def genpat_detect_all(timeout_sec: float,
                     pattern_before: Path,
                     pattern_after: Path,
                     repo_path: Path,
                    result_path: Path,
                    jar_path: Path):
    work_dir = jar_path.parent

    cmd = ["java",
           "-Dfile.encoding=utf-8",
           "-jar", str(jar_path), "matchAll", str(pattern_before), str(pattern_after), str(repo_path), str(result_path)]

    start_process(cmd, work_dir, timeout_sec)

