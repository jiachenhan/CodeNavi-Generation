from abc import ABC, abstractmethod
from functools import cached_property
from pathlib import Path
from typing import Generator, List, Dict, Union, Optional


class DataCollection:
    def __init__(self,
                 base_path: Path,
                 names: Optional[List[str]],
                 grouped: bool = True):
        self.collection_name: str = base_path.stem
        self.base_path: Path = base_path
        self.grouped = grouped
        if names is None:
            self.names = list(map(lambda path: path.stem, self.base_path.iterdir()))
        else:
            self.names = names

    @cached_property
    def datasets(self):
        return self._get_datasets()

    def _get_datasets(self):
        for dataset_path in self.base_path.iterdir():
            # if self.names is None:
            #     yield self._create_dataset(dataset_path)
            # else:
            if dataset_path.stem in self.names:
                yield self._create_dataset(dataset_path)

    def _create_dataset(self, dataset_path: Path):
        if self.grouped:
            return GroupedDataset(dataset_path)
        else:
            return UnGroupedDataset(dataset_path)

    def __str__(self):
        return f"DataCollection(grouped={self.grouped}, name={self.collection_name})"


class OneMethodFilePair:
    """must process to one method warped with a class file"""
    def __init__(self,
                 dataset_path: Path,
                 relative_path: Path,
                 before_java: str = "error.java",
                 after_java: str = "correct.java") -> None:
        self.dataset_path = dataset_path
        self.relative_path = relative_path  # from dataset to cases
        self.case_path: Path = dataset_path / relative_path
        self.before_path: Path = self.case_path / before_java
        self.after_path: Path = self.case_path / after_java

    @classmethod
    def from_before(cls, dataset_path: Path, before_path: Path):
        relative_path = before_path.parent.relative_to(dataset_path)
        return cls(dataset_path, relative_path)

    @classmethod
    def from_case(cls, dataset_path: Path, case_path: Path):
        relative_path = case_path.relative_to(dataset_path)
        return cls(dataset_path, relative_path)


class Dataset(ABC):
    def __init__(self, dataset_path: Path) -> None:
        self.dataset_path = dataset_path
        self.name = dataset_path.name

    def _get_groups(self):
        return filter(lambda file: file.is_dir(), self.dataset_path.iterdir())

    @abstractmethod
    def get_datas(self) -> Generator[Union[OneMethodFilePair], Dict[str, List[OneMethodFilePair]], None, None]:
        """对文件对进行抽象，具体操作由子类实现"""
        pass


class GroupedDataset(Dataset):
    def __init__(self,
                 dataset_path: Path):
        super().__init__(dataset_path)

    def get_datas(self) -> Generator[Dict[str, List[OneMethodFilePair]], None, None]:
        for group in self._get_groups():
            pairs = []
            for case in filter(lambda file: file.is_dir(), group.iterdir()):
                pairs.append(OneMethodFilePair.from_case(self.dataset_path, case))
            yield group.stem, pairs


class UnGroupedDataset(Dataset):
    def __init__(self,
                 dataset_path: Path):
        super().__init__(dataset_path)

    def get_datas(self) -> Generator[OneMethodFilePair, None, None]:
        for group in self._get_groups():
            yield OneMethodFilePair.from_case(self.dataset_path, group)


if __name__ == "__main__":
    # cluster_path = Path("/data/jiangjiajun/LLMPAT/temporary/c3")
    cluster_path = Path("E:/dataset/c3")
    dataset_names = ["ant1", "cobertura1", "drjava1", "fitlibrary1", "jgrapht1", "junit1", "checkstyle1"]
    data_collection = DataCollection(cluster_path, names=None, grouped=True)
    for dataset in data_collection.datasets:
        print(dataset.dataset_path)
        if dataset.name == "fitlibrary1":
            for group_stem, pair_list in dataset.get_datas():
                if group_stem == "1103":
                    print("here")
                print(group_stem)
                if len(pair_list) <= 1:
                    print("less than 1 pair")
                for pair in pair_list:
                    print(pair.before_path)
                    print(pair.after_path)

    print("here")
