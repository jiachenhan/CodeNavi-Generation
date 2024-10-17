class LLMAPI:
    def __init__(self, base_url, api_key):
        self.base_url = base_url
        self.api_key = api_key

    def invoke(self, messages):
        raise NotImplementedError("This method should be overridden by subclasses.")