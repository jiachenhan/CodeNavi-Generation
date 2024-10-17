from prettytable import PrettyTable

from utils.config import LoggerConfig
from utils.singleton_meta import SingletonMeta

_logger = LoggerConfig.get_logger(__name__)

# $x / 1M tokens
TOKEN_COSTS = {
    "CodeLlama": {"prompt": 0.0, "completion": 0.0},
    "deepseek-chat": {"prompt_hit": 0.014, "prompt_miss": 0.14, "completion": 0.28},

    "gpt-3.5-turbo-instruct": {"prompt": 1.5, "completion": 2.0},
    "gpt-4-turbo": {"prompt": 10.0, "completion": 30.0},
    "gpt-4": {"prompt": 30.0, "completion": 60.0},
    "gpt-4-32k": {"prompt": 60.0, "completion": 120.0},
    "gpt-4o-mini": {"prompt": 0.15, "completion": 0.6},
    "gpt-4o": {"prompt": 5.0, "completion": 15.0},
    "text-embedding-ada-002": {"prompt": 0.4, "completion": 0.0},
}


class CostManager:
    def __init__(self, model_name: str):
        self.model_name = model_name
        self.tokens = {
            "cost_model": "ALL",  # ALL, normal, cache\
            "normal": {
                "prompt": 0,
                "completion": 0,
            },
            "cache": {
                "prompt_hit": 0,
                "prompt_miss": 0,
                "completion": 0,
            },
            "cost": 0,
        }

    def _clear_token(self):
        self.tokens["normal"]["prompt"] = 0
        self.tokens["normal"]["completion"] = 0
        self.tokens["cache"]["prompt_hit"] = 0
        self.tokens["cache"]["prompt_miss"] = 0
        self.tokens["cache"]["completion"] = 0
        self.tokens["cost"] = 0

    def update_all_cost(self, cost_managers: list):
        if self.model_name == "ALL":
            self._clear_token()
            for cost_manager in cost_managers:
                if cost_manager.tokens["cost_model"] == "normal":
                    self.tokens["normal"]["prompt"] += cost_manager.tokens["normal"]["prompt"]
                    self.tokens["normal"]["completion"] += cost_manager.tokens["normal"]["completion"]
                elif cost_manager.tokens["cost_model"] == "cache":
                    self.tokens["cache"]["prompt_hit"] += cost_manager.tokens["cache"]["prompt_hit"]
                    self.tokens["cache"]["prompt_miss"] += cost_manager.tokens["cache"]["prompt_miss"]
                    self.tokens["cache"]["completion"] += cost_manager.tokens["cache"]["completion"]
                self.tokens["cost"] += cost_manager.tokens["cost"]
        else:
            _logger.error("update_all_cost is only available for model_name=ALL")

    def update_cost(self, *args, **kwargs):
        prompt_tokens = 0
        prompt_hit = 0
        prompt_miss = 0
        completion_tokens = 0

        if len(args) == 2:
            self.tokens["cost_model"] = "normal"
            prompt_tokens, completion_tokens = args
        elif len(args) == 3:
            self.tokens["cost_model"] = "cache"
            prompt_hit, prompt_miss, completion_tokens = args

        if self.tokens["cost_model"] == "normal":
            self.tokens["normal"]["prompt"] += prompt_tokens
            self.tokens["normal"]["completion"] += completion_tokens
            self.tokens["cost"] = (
                    prompt_tokens * TOKEN_COSTS[self.model_name]["prompt"] +
                    completion_tokens * TOKEN_COSTS[self.model_name]["completion"]
            ) / 1000 / 1000
        elif self.tokens["cost_model"] == "cache":
            self.tokens["cache"]["prompt_hit"] += prompt_hit
            self.tokens["cache"]["prompt_miss"] += prompt_miss
            self.tokens["cache"]["completion"] += completion_tokens
            self.tokens["cost"] = (
                    prompt_hit * TOKEN_COSTS[self.model_name]["prompt_hit"] +
                    prompt_miss * TOKEN_COSTS[self.model_name]["prompt_miss"] +
                    completion_tokens * TOKEN_COSTS[self.model_name]["completion"]
            ) / 1000 / 1000

    def show_cost(self):
        table = PrettyTable()
        if self.tokens["cost_model"] == "normal":
            table.field_names = ["Name", "prompt tokens", "completion tokens", "cost"]
            table.add_row([self.model_name, self.tokens["normal"]["prompt"], self.tokens["normal"]["completion"],
                           self.tokens["cost"]])
        elif self.tokens["cost_model"] == "cache":
            table.field_names = ["Name", "prompt hit", "prompt miss", "completion tokens", "cost"]
            table.add_row([self.model_name, self.tokens["cache"]["prompt_hit"], self.tokens["cache"]["prompt_miss"],
                           self.tokens["cache"]["completion"], self.tokens["cost"]])
        elif self.tokens["cost_model"] == "ALL":
            table.field_names = ["Name", "prompt tokens", "normal completion tokens",
                                 "prompt hit", "prompt miss",
                                 "cache completion tokens", "cost"]
            table.add_row(["ALL", self.tokens["normal"]["prompt"], self.tokens["normal"]["completion"],
                           self.tokens["cache"]["prompt_hit"], self.tokens["cache"]["prompt_miss"],
                           self.tokens["cache"]["completion"], self.tokens["cost"]])
        else:
            _logger.error(f"cost model {self.tokens['cost_model']} is not supported")
        _logger.info(f"cost table:\n{table}")
