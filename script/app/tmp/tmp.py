from pathlib import Path

if __name__ == "__main__":
    target_path = Path("/data/jiangjiajun/CodeNavi-DSL/CodeNavi-Generation/02pattern-info/codeql_hiera_60")

    for wrong_file in target_path.rglob("*.ser"):
        try:
            # 构建新路径（替换后缀）
            new_path = wrong_file.with_suffix(".json")
            # 执行重命名
            wrong_file.rename(new_path)
            print(f"成功: {wrong_file.name} -> {new_path.name}")
        except Exception as e:
            print(f"失败: {wrong_file.name} | 错误: {str(e)}")