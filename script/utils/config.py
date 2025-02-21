import os

import yaml
from pathlib import Path
import logging
from colorlog import ColoredFormatter

from utils.singleton_meta import SingletonMeta


class LoggerConfig:
    @classmethod
    def get_logger(cls, name=__name__, level=logging.DEBUG):
        logger = logging.getLogger(name)
        color_format = '%(log_color)s%(levelname)-8s%(reset)s %(blue)s%(message)s'
        formatter = ColoredFormatter(color_format)

        stream = logging.StreamHandler()
        stream.setLevel(level)
        stream.setFormatter(formatter)

        logger.addHandler(stream)
        logger.setLevel(logging.DEBUG)
        return logger


class YamlConfig(metaclass=SingletonMeta):
    def __init__(self, file_path=Path(__file__).parents[2].joinpath("06config", 'config.yml')):
        if not file_path.exists():
            LoggerConfig.get_logger(__file__).error("Config file not exist")
        with open(file_path, 'r', encoding="utf-8") as file:
            self.config = yaml.safe_load(file)

    def get_config(self):
        return self.config


_config = YamlConfig().get_config()

def set_config(tag: str = "silicon"):
    if tag == "huawei":
        huawei_config()
    elif tag == "deepseek":
        deepseek_config()
    elif tag == "silicon":
        silicon_config()
    elif tag == "ppinfra":
        ppinfra_config()

def huawei_config():
    os.environ["OPENAI_API_KEY"] = _config["huawei"]["API_KEY"][0]
    os.environ["OPENAI_BASE_URL"] = _config["huawei"]["BASE_URL"]
    os.environ["MODEL_NAME"] = _config["huawei"]["MODEL_NAME"]
    os.environ["NO_PROXY"] = _config["huawei"]["NO_PROXY"]

    os.environ['jar_path'] = _config["jar_path"]

def deepseek_config():
    os.environ["OPENAI_API_KEY"] = _config["tju"]["deepseek"]["API_KEY"][0]
    os.environ["OPENAI_BASE_URL"] = _config["tju"]["deepseek"]["BASE_URL"]
    os.environ["MODEL_NAME"] = _config["tju"]["deepseek"]["MODEL_NAME"]
    os.environ['HTTP_PROXY'] = _config["tju"]["HTTP_PROXY"]
    os.environ['HTTPS_PROXY'] = _config["tju"]["HTTPS_PROXY"]

    os.environ['jar_path'] = _config["tju"]["jar_path"]


def silicon_config():
    os.environ["OPENAI_API_KEY"] = _config["tju"]["silicon_flow"]["API_KEY"][0]
    os.environ["OPENAI_BASE_URL"] = _config["tju"]["silicon_flow"]["BASE_URL"]
    os.environ["MODEL_NAME"] = _config["tju"]["silicon_flow"]["MODEL_NAME"]
    os.environ['HTTP_PROXY'] = _config["tju"]["HTTP_PROXY"]
    os.environ['HTTPS_PROXY'] = _config["tju"]["HTTPS_PROXY"]

    os.environ['jar_path'] = _config["tju"]["jar_path"]

def ppinfra_config():
    os.environ["OPENAI_API_KEY"] = _config["tju"]["ppinfra"]["API_KEY"][0]
    os.environ["OPENAI_BASE_URL"] = _config["tju"]["ppinfra"]["BASE_URL"]
    os.environ["MODEL_NAME"] = _config["tju"]["ppinfra"]["MODEL_NAME"]
    os.environ['HTTP_PROXY'] = _config["tju"]["HTTP_PROXY"]
    os.environ['HTTPS_PROXY'] = _config["tju"]["HTTPS_PROXY"]

    os.environ['jar_path'] = _config["tju"]["jar_path"]

def get_jar_path() -> str:
    return os.environ.get("jar_path")


# 指代FixGen
def get_root_project_path() -> Path:
    return Path(__file__).parents[2]


def get_pattern_base_path() -> Path:
    return get_root_project_path().joinpath("01pattern")


def get_pattern_info_base_path() -> Path:
    return get_root_project_path().joinpath("02pattern-info")


def get_patches_base_path() -> Path:
    return get_root_project_path().joinpath("04patch")


def get_dsl_base_path() -> Path:
    return get_root_project_path().joinpath("07dsl")


def get_random_seed() -> int:
    return 42

class PipelineConfig:
    dataset_name = "user_dataset"
    group = "user_case"
    jar_path = get_jar_path()
    pattern_ori_path = get_pattern_base_path() / dataset_name / "ori" / f"{group}.ser"
    pattern_abs_path = get_pattern_base_path() / dataset_name / "abs" / f"{group}.ser"
    pattern_info_path = get_pattern_info_base_path() / dataset_name / "input" / f"{group}.ser"
    pattern_output_path = get_pattern_info_base_path() / dataset_name / "output" / f"{group}.ser"


if __name__ == "__main__":
    print(get_root_project_path())
    config_instance = YamlConfig()
    config = config_instance.get_config()
    print(config)
    # print(get_llm_project_path())
    # print(get_dataset_base_path())
    # print(get_dataset_names())
    # print(type(get_dataset_names()))
    # print(get_repair_use_pattern_base_path())

