from openai import OpenAI


def test_service():
    try:
        client = OpenAI(
            base_url = "http://localhost:8001/v1",
            api_key = "empty"
        )

        # 创建聊天请求
        response = client.chat.completions.create(
            model="CodeLlama",  # 替换为实际模型名称
            messages=[
                {"role": "system", "content": "You are a helpful assistant."},
                {"role": "user", "content": "Hello, world!"}
            ]
        )

        # 打印响应
        print("服务正常运行:", response.choices[0].message.content)
    except Exception as e:
        print("请求失败:", e)

if __name__ == "__main__":
    test_service()