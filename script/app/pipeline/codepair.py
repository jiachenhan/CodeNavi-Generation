import argparse
from datetime import datetime
from pathlib import Path

from app.pipeline.abstract import do_abstract
from app.pipeline.extract import do_extract
from app.pipeline.genquery import do_generate_dsl
from utils.config import LoggerConfig


def parse_args():
    parser = argparse.ArgumentParser(description="通过CLI参数接收codepair路径和生成规则路径")

    parser.add_argument(
        '--code_path',
        type=str,
        required=True,
        help='输入文件的路径'
    )

    parser.add_argument(
        '--dsl_path',
        type=str,
        required=True,
        help='生成dsl的路径'
    )

    args = parser.parse_args()

    return args.code_path, args.dsl_path


if __name__ == "__main__":
    code_path, dsl_path = parse_args()

    log = LoggerConfig.get_logger()

    log.info("Starting...")
    log.info(datetime.now())

    dataset_path = Path(code_path)
    log.info(f"dataset_path: {dataset_path}")

    log.info("Starting extract ast...")
    log.info(datetime.now())
    do_extract(dataset_path)

    log.info("Starting abstract pattern...")
    log.info(datetime.now())
    do_abstract()

    log.info("Starting generate dsl...")
    log.info(datetime.now())
    do_generate_dsl(dsl_path)

    log.info(datetime.now())
    log.info("Finished...")