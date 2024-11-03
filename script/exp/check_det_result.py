import json
from pathlib import Path


def check_dataset_result(_dataset_path: Path, result_path: Path):
    total_group_num = len(list(_dataset_path.iterdir()))

    detected_group_num = 0
    correct_group_num = 0
    average_right_num = 0
    average_det_nul = 0
    for group_path in result_path.iterdir():
        detected_group_num += 1
        for result_file in group_path.iterdir():
            if result_file.suffix == ".json":
                det, det_num = parse_result_file(result_file)
                average_det_nul += det_num
                if det:
                    correct_group_num += 1
                    average_right_num += det_num
            else:
                print(f"Invalid file: {result_file}")
                break
    print(f"Dataset: {result_path.name}")
    print(f"Total group num: {total_group_num}")
    print(f"Detected group num: {detected_group_num}")
    print(f"Correct group num: {correct_group_num}")
    print(f"detected rate: {correct_group_num / total_group_num}")
    print(f"Average right num: {average_right_num / correct_group_num}")
    print(f"Average det num: {average_det_nul / detected_group_num}")


def parse_result_file(result_file: Path):
    with result_file.open() as f:
        result = json.load(f)
        detected = result["detected"]
        det_num = len(result["results"])
        return detected, det_num


if __name__ == "__main__":
    datasets_path = Path("/data/jiangjiajun/LLMPAT/dataset/c3_random_1000")
    results_path = Path("/data/jiangjiajun/LLMPAT/ModifyMeta/04patch/detect/c3_random_1000/abs_pat")
    for dataset_path in datasets_path.iterdir():
        check_dataset_result(dataset_path, results_path / dataset_path.name)
