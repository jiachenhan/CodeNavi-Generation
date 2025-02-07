import utils.config

if __name__ == '__main__':
    jar_path = utils.config.get_jar_path()

    dataset_name = "sample_100_dataset"
    group_names = []

    for group_name in group_names:

        pattern_ori_ser_path = utils.config.get_pattern_base_path() / "ori" / dataset_name / f"{group_name}.ser"
        pattern_ori_ser_path.parent.mkdir(parents=True, exist_ok=True)
