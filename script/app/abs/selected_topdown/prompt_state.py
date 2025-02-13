from utils.config import LoggerConfig


_logger = LoggerConfig.get_logger(__name__)

STRUCTURE_RELATED_AST_NODE_TYPES = ["MoBlock", "MoDoStatement", "MoEnhancedForStatement", "MoForStatement",
                                    "MoIfStatement", "MoSwitchStatement", "MoSynchronizedStatement",
                                    "MoWhileStatement", "MoTryStatement", "MoTypeDeclarationStatement"]

NAME_AST_NODE_TYPES = ["MoSimpleName", "MoQualifiedName",
                       "MoBooleanLiteral", "MoCharacterLiteral", "MoNullLiteral",
                       "MoNumberLiteral", "MoStringLiteral", "MoTypeLiteral"]

class PromptState:
    def __init__(self, analyzer):
        self.analyzer = analyzer

    def accept(self):
        raise NotImplementedError

class InitialState(PromptState):
    def accept(self):
        self.analyzer.prompt_state = BackGroundState(self.analyzer)


class ExitState(PromptState):
    def accept(self):
        pass


class BackGroundState(PromptState):
    def accept(self):
        pass