from pathlib import Path

from utils.config import LoggerConfig

_logger = LoggerConfig.get_logger(__name__)

def detect_single(_data_path: Path, _result_path: Path):
    engine_path = Path("D:/env/kirin-cli-1.0.8_sp06-jackofall.jar")
    _manual_dsl_path = _data_path / "dsl.kirin"
    code_repo = next(filter(lambda f: f.isdir(), _data_path.iterdir()), None)
    if code_repo is None:
        _logger.error(f"{_data_path} don't have a valid repo")
        return



    pass


def detect_repos(_dataset_path: Path, _results_path: Path):
    for _data_path in _dataset_path.iterdir():
        _result_path = _results_path / _data_path.stem
        detect_single(_data_path, _result_path)


if __name__ == '__main__':
    dataset_name = "inner_18"
    dataset_path = Path("D:/datas/") / dataset_name

    results_path = Path(f"D:/datas/result_manual_repo_{dataset_name}")
    result_store_path = results_path / "manual_result_store.json"

    detect_repos(dataset_path, results_path)

    # results = navi_repo_statistic(dataset_path, results_path)

    # with open(result_store_path, "w", encoding="utf-8") as file:
    #     json.dump(results, file, indent=4)