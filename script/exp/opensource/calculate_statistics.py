import re
import glob
import json
from pathlib import Path
from bs4 import BeautifulSoup


def read_from_json(result_file: Path):
    results = json.load(open(result_file, 'r'))
    ret_dict = {}
    for dict in results:
        task_info = dict["result_path"].split("result_trans_repo_")[1]
        if "\\" in task_info:
            task_info = task_info.split("\\")[1:4]
        else:
            task_info = task_info.split("/")[1:4]
        task_id = task_info[0] + "#" + task_info[1] + "#" + task_info[2]
        ret_dict[task_id] = dict["result"]
    return ret_dict


def metric_scanned_amount(navi_dict, genpat_dict):
    navi_amounts = []
    genpat_amounts = []
    better_count = 0
    worse_count = 0
    genpt_zero_count = 0
    for key in navi_result.keys():
        navi_amounts.append(navi_result[key]["all_scanned"])
        if key not in genpat_result.keys():
            genpat_amounts.append(0)            
            genpt_zero_count += 1
        else:
            if genpat_result[key]["no_pattern"]:
                genpat_amounts.append(-1)
            else:
                genpat_amounts.append(genpat_result[key]["all_scanned"])
                

    print(f"{genpt_zero_count} results of genpat is not exist...")

    for i in range(len(navi_amounts)):
        if genpat_amounts[i] == -1:
            continue
        if navi_amounts[i] > genpat_amounts[i]:
            worse_count += 1
        else:
            better_count += 1
    genpat_amounts = [item for item in genpat_amounts if item != -1]
    print(f"better count: {better_count}, worse count: {worse_count}")
    print(f"Navi info: min:{min(navi_amounts)}, max:{max(navi_amounts)}, avg:{(sum(navi_amounts) / len(navi_amounts)): .2f}")
    print(f"GenPat info: min:{min(genpat_amounts)}, max:{max(genpat_amounts)}, avg:{(sum(genpat_amounts) / len(genpat_amounts)): .2f}")
    pass


def metric_recall(navi_dict, genpat_dict):
    navi_tp = 0
    genpat_tp = 0
    for key in navi_result.keys():
        if navi_result[key]["recall"]:
            navi_tp += 1
        if key in genpat_dict.keys():
            genpat_tp += 1 if genpat_dict[key]["recall"] else 0
    print(f"navi_tp:{navi_tp}, genpt_tp: {genpat_tp}")
    pass


def find_navi_warnings(_dataset_name: str, navi_results_path: Path):
    xml_files = glob.glob(f"{navi_results_path}/*/*.xml")
    ret_warnings = []
    # print(f"xml_files: {len(xml_files)}")
    for xml_file in xml_files:
        with open(xml_file, 'r', encoding='ISO-8859-1') as file:
            xml_content = file.read()
        soup = BeautifulSoup(xml_content, 'xml')
        error_tags = soup.find_all("error")
        # if len(error_tags) != 0:
        #     print(f"find_navi_warnings: {len(error_tags)}")
        for error in error_tags:
            detect_info = error.find("defectInfo")
            # print(detect_info.fileName.text.replace("\\", "/"))
            file_name = "/".join(detect_info.fileName.text.replace("\\", "/").split(_dataset_name)[1].split("/")[5:]) if detect_info.fileName else None
            function = detect_info.function.text if detect_info.function else None
            det_line = detect_info.reportLine.text
            ret_warnings.append((file_name + "#" + function, det_line))
    # print(f"find_navi_warnings: {len(ret_warnings)}")
    # print(f"ret_warnings: {ret_warnings}")
    # input('test')
    return ret_warnings


def find_genpat_warnings(_dataset_name: str, genpat_results_path: Path):
    result_file = genpat_results_path / "result.txt"
    lines = open(result_file, 'r', encoding="ISO-8859-1").readlines()
    ret_warnings = []
    for line in lines:
        line = line.strip()
        line = re.sub(r"\t+", " ", line)
        if len(line) == 0 or line.startswith("Empty Pattern"):
            continue
        info = line.split("det ")[1].strip()
        file_path = info.split(" ")[0].replace("\\", "/")
        # file_path = "/".join(file_path.split("_sampled_v1_")[1].split("/")[5:])
        # sig = " ".join(info.split(" ")[1:]).split(" ")[1].split("[")[0]
        # ret_warnings.append(file_path + "#" + sig)
        file_path = "/".join(file_path.split(_dataset_name)[1].split("/")[5:])
        sig_name = " ".join(info.split(" ")[1:]).split(" ")[1].split("[")[0]
        ret_type = info.split(" ")[1].strip()
        params = " ".join(info.split(" ")[1:]).split("[")[1].split("]")[0].strip()
        ret_warnings.append((file_path, sig_name, ret_type, params))
    return ret_warnings


def get_baseline_warnings(baseline_case_warnings: Path):
    if not baseline_case_warnings.exists():
        print(f"{baseline_case_warnings} not exist!!!")
        return []
    warning_files = glob.glob(f"{baseline_case_warnings}/*warnings.txt")
    assert len(warning_files) == 1
    lines = open(warning_files[0], 'r', encoding="ISO-8859-1").readlines()
    ret_warnings = []
    for line in lines:
        if not line.startswith("hash#"):
            continue
        _, file_path, begin_line, end_line, sig = line.strip().split("#")
        # sig = " ".join(sig.split(" ")[1:]).split("(")[0].strip()
        # ret_warnings.append(file_path + "#" + sig + "#" + begin_line + "#" + end_line)
        params = sig.split("(")[1].split(")")[0].strip()
        sig_name = " ".join(sig.split(" ")[1:]).split("(")[0].strip()
        ret_warnings.append((file_path, sig_name, params, begin_line, end_line))
    return ret_warnings


def metric_coverage(_dataset_name: str, baseline_warnings: Path, result_dict: dict, warnings_path: Path, output_path: Path, is_navi=True):
    ret_result_dict = {}
    for task_id in result_dict.keys():
        checker_name, group_id, case_id = task_id.split("#")
        src_case_id, dst_case_id = case_id.split("-")
        baseline_case_warnings = get_baseline_warnings(baseline_warnings / checker_name / group_id / dst_case_id)
        if len(baseline_case_warnings) == 0:
            continue
        tool_case_warnings_path = warnings_path / checker_name / group_id /case_id
        if is_navi:
            tool_warnings = find_navi_warnings(_dataset_name, tool_case_warnings_path)
        else:
            tool_warnings = find_genpat_warnings(_dataset_name, tool_case_warnings_path)

        detect_amount = 0
        # print(tool_warnings)
        # print(baseline_case_warnings)
        if is_navi:
            for warning in tool_warnings:
                warn_file, line = warning
                for baseline_warning in baseline_case_warnings:
                    file_path, sig_name, params, begin_line, end_line = baseline_warning
                    if file_path in warn_file and int(begin_line) <= int(line) <= int(end_line):
                        detect_amount += 1
                        break
        else:
            for file_path, sig_name, ret_type, params in tool_warnings:
                for baseline_file_path, baseline_sig_name, baseline_params, baseline_begin_line, baseline_end_line in baseline_case_warnings:
                    if file_path == baseline_file_path and sig_name == baseline_sig_name and params == baseline_params:
                        detect_amount += 1
                        break


        print(f"{task_id}: {detect_amount}/{len(baseline_case_warnings)}")
        # input("Press Enter to continue...")
        ret_result_dict[task_id] = f"{detect_amount}/{len(baseline_case_warnings)}"
    with open(output_path, 'w') as f:
        f.write(json.dumps(ret_result_dict, indent=4))
    pass


def analyze_coverage(coverage_report: Path):
    json_dict = json.load(open(coverage_report, 'r'))
    all_coverage = 0
    all_tested = 0
    all_coverage_rates = []
    for key in json_dict.keys():
        coverage = json_dict[key]
        tested, total = coverage.split("/")
        tested = int(tested)
        total = int(total)
        all_coverage += tested
        all_tested += total
        all_coverage_rates.append(tested / total)
    print(f"avg coverage: {all_coverage / all_tested}")
    return all_coverage_rates  


def draw_cov_box_plot(_dataset_name, data1, data2):
    import matplotlib.pyplot as plt
    import seaborn as sns
    import pandas as pd
    # Create DataFrame
    df = pd.DataFrame({
        'Group': ['Navi']*len(data1) + ['GenPat']*len(data2),
        'Value': data1 + data2
    })

    plt.figure(figsize=(8, 6))

    # Create box plot
    sns.boxplot(x='Group', y='Value', data=df, width=0.4, 
                palette=['lightblue', 'lightgreen'])

    # Add strip plot with jitter
    sns.swarmplot(x='Group', y='Value', data=df, 
                color='black', alpha=0.7, size=6)

    plt.title('Repo Coverage Rate Comparison')
    plt.ylabel('Coverage Rate (%)')
    # plt.show()
    plt.savefig(f"./{_dataset_name}_cov_box_plot.png", dpi=300)

def filter_outliers(data):
    import numpy as np
    q1 = np.percentile(data, 25)
    q3 = np.percentile(data, 75)
    iqr = q3 - q1
    lower_bound = q1 - 1.5 * iqr
    upper_bound = q3 + 1.5 * iqr
    filtered_data = [x for x in data if lower_bound <= x <= upper_bound]
    return filtered_data

def draw_num_box_plot(_dataset_name, data1, data2):
    import matplotlib.pyplot as plt
    import seaborn as sns
    import pandas as pd

    data = [data1, data2]
    # 绘制盒图
    plt.boxplot(data)

    # 设置x轴标签（可选）
    plt.xticks([1, 2], ['Navi', 'GenPat'])

    # 添加标题和标签（可选）
    plt.title("det nums")
    plt.xlabel("methods")
    plt.ylabel("nums")

    # 显示图形
    # plt.show()
    plt.savefig(f"./{_dataset_name}_num_box_plot.png", dpi=300)


if __name__ == "__main__":
    dataset_name = "codeql_sampled_v1"
    navi_result = read_from_json(Path(f"E:/dataset/Navi/2_result_trans_repo_{dataset_name}/navi_result_store.json"))
    genpat_result = read_from_json(Path(f"E:/dataset/Navi/2_consistent_genpat_result_trans_repo_{dataset_name}/genpat_result_store.json"))
    
    # metric_scanned_amount(navi_dict=navi_result, genpat_dict=genpat_result)
    # metric_recall(navi_dict=navi_result, genpat_dict=genpat_result)
    # navi_det_num = filter_outliers([result["all_scanned"] for result in navi_result.values()])
    # genpat_det_num = filter_outliers([result["all_scanned"] for result in genpat_result.values()])
    # draw_num_box_plot(dataset_name, data1=navi_det_num, data2=genpat_det_num)


    # metric_coverage(dataset_name,
    #                 baseline_warnings=Path(f"E:/dataset/Navi/{dataset_name}_repos111"),
    #                 result_dict=navi_result,
    #                 warnings_path=Path(f"E:/dataset/Navi/2_result_trans_repo_{dataset_name}"),
    #                 output_path=Path(f"E:/dataset/Navi/navi_RQ2_{dataset_name}_coverage_report.json"),
    #                 is_navi=True)
    # metric_coverage(dataset_name,
    #                 baseline_warnings=Path(f"E:/dataset/Navi/{dataset_name}_repos111"),
    #                 result_dict=genpat_result,
    #                 warnings_path=Path(f"E:/dataset/Navi/2_consistent_genpat_result_trans_repo_{dataset_name}"),
    #                 output_path=Path(f"E:/dataset/Navi/genpat_RQ2_{dataset_name}_coverage_report.json"),
    #                 is_navi=False)
    # navi_coverage_rate = analyze_coverage(coverage_report=Path(f"E:/dataset/Navi/navi_RQ2_{dataset_name}_coverage_report.json"))
    # genpat_coverage_rate = analyze_coverage(coverage_report=Path(f"E:/dataset/Navi/genpat_RQ2_{dataset_name}_coverage_report.json"))
    # draw_cov_box_plot(dataset_name, data1=navi_coverage_rate, data2=genpat_coverage_rate)

    navi_coverage_rate = analyze_coverage(coverage_report=Path(f"E:/dataset/Navi/navi_merge.json"))
    genpat_coverage_rate = analyze_coverage(coverage_report=Path(f"E:/dataset/Navi/genpat_merge.json"))
    draw_cov_box_plot("merge_ql", data1=navi_coverage_rate, data2=genpat_coverage_rate)
    pass
