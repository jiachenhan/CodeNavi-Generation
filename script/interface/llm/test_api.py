import os

from openai import OpenAI

from utils.config import set_config

if __name__ == "__main__":
    set_config("deepseek")
    # key = os.environ.get("OPENAI_API_KEY")
    # url = os.environ.get("OPENAI_BASE_URL")
    # name = os.environ.get("MODEL_NAME")

    key = "sk-92e516aab3d443adb30c6659284163e8"
    url = "https://api.deepseek.com"
    name = "deepseek-chat"

    client = OpenAI(api_key="sk-92e516aab3d443adb30c6659284163e8", base_url="https://api.deepseek.com")

    response = client.chat.completions.create(
        model="deepseek-chat",
        messages=[
            {"role": "system", "content": "You are a helpful assistant"},
            {"role": "user", "content": "Hello"},
        ],
        stream=False
    )

    print(response.choices[0].message.content)