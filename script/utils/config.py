import yaml
from pathlib import Path
import logging
from colorlog import ColoredFormatter


class SingletonMeta(type):
    _instances = {}

    def __call__(cls, *args, **kwargs):
        if cls not in cls._instances:
            instance = super().__call__(*args, **kwargs)
            cls._instances[cls] = instance
        return cls._instances[cls]


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


# 指代FixGen
def get_root_project_path() -> Path:
    return Path(__file__).parents[2]


def get_patches_base_path() -> Path:
    return get_root_project_path().joinpath("04patch")


def get_random_seed() -> int:
    return 42


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

