import json
from pathlib import Path
from typing import Generator

from utils.common import reservoir_sampling


def filter_repos(results: dict) -> Generator[dict, None, None]:
    def filter_func(x):
        return x["languages"]["Java"] > 20000 and not x["isArchived"]

    for item in results["items"]:
        if filter_func(item):
            yield item


def main():
    target_path = Path("E:/dataset/Navi/vul/repos")

    results = json.load(open("repo_results.json", 'r', encoding='utf-8'))
    repos = filter_repos(results)
    for index, repo in enumerate(reservoir_sampling(repos, 100)):
        print(index)
        print(repo["name"])


if __name__ == "__main__":
    main()
