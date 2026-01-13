"""get blamed version warning"""
from pathlib import Path
from typing import Generator

import paramiko

path_mapping = {
    "codeql_v1_commits": "codeql_v1_commits_ori",
    "codeql_v2_commits": "codeql_v2_commits_ori",
    "pmd_v1_commits": "pmd_v1_commits_ori",
    "pmd_v2_commits": "pmd_v2_commits_ori"
}

target_files = ["sat_warnings.json"]

# 全局变量（根据你的实际情况设置）
username = "jiangjiajun"
hostname = "172.28.102.8"
base_local_path = Path("E:/dataset/Navi/rq2_commit")
base_remote_path = Path("/data/jiangjiajun/DSL-AutoDebug")

# 创建 SSH 客户端
ssh_client = paramiko.SSHClient()
ssh_client.set_missing_host_key_policy(paramiko.AutoAddPolicy())


def connect_ssh():
    """建立 SSH 连接"""
    try:
        # 尝试密钥认证（推荐）
        ssh_client.connect(
            hostname=hostname,
            username=username,
            timeout=30
        )
        print(f"Connected to {username}@{hostname}")
        return True
    except Exception as e:
        print(f"Connection failed: {e}")
        return False


def remote_file_exists(remote_path):
    """检查远程文件是否存在 - 替换原来的 subprocess 版本"""
    try:
        # 使用 SFTP 的 stat 方法来检查文件是否存在
        sftp = ssh_client.open_sftp()
        try:
            sftp.stat(remote_path)
            return True
        except FileNotFoundError:
            return False
        finally:
            sftp.close()
    except Exception as e:
        print(f"Error checking remote file {remote_path}: {e}")
        return False


def scp_file(remote_path, local_path):
    """SCP 文件传输 - 替换原来的 subprocess 版本"""
    try:
        # 确保本地目录存在
        local_path = Path(local_path)
        local_path.parent.mkdir(parents=True, exist_ok=True)

        # 使用 SFTP 下载文件
        sftp = ssh_client.open_sftp()

        print(f"Downloading {remote_path} to {local_path}")
        sftp.get(remote_path, str(local_path))
        sftp.close()

        print(f"Successfully copied {remote_path} to {local_path}")
        return True

    except Exception as e:
        print(f"Error copying {remote_path} to {local_path}: {e}")
        return False


# 你的原始逻辑保持不变
def find_case_dirs(root_path) -> Generator[Path, None, None]:
    root = Path(root_path)
    # 遍历 checker_name 目录
    for checker_dir in root.iterdir():
        if not checker_dir.is_dir():
            continue

        # 遍历 group 目录
        for group_dir in checker_dir.iterdir():
            if not group_dir.is_dir():
                continue

            # 遍历 case 目录
            for case_dir in group_dir.iterdir():
                if not case_dir.is_dir():
                    continue
                yield case_dir


if __name__ == "__main__":
    # 建立连接
    if not connect_ssh():
        print("Failed to connect to remote host")
        exit(1)

    try:
        for local_dir, remote_dir in path_mapping.items():
            local_root = base_local_path / local_dir
            remote_root = base_remote_path / remote_dir

            if not local_root.exists():
                print(f"Local directory does not exist: {local_root}")
                continue

            for case_dir in find_case_dirs(local_root):
                for fname in target_files:
                    local_file = case_dir / fname
                    # 构建远程文件路径
                    remote_relative = case_dir.relative_to(local_root)
                    remote_file = remote_root / remote_relative / fname
                    remote_file_str = str(remote_file).replace("\\", "/")

                    if not local_file.exists():
                        if remote_file_exists(remote_file_str):
                            print(f"Copying {remote_file_str} to {local_file}")
                            scp_file(remote_file_str, local_file)

    finally:
        # 关闭连接
        ssh_client.close()
        print("SSH connection closed")
