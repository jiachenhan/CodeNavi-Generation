package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class FunctionDeclaration extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "functionDeclaration";
    }

    @Override
    public String getAlias() {
        return "funDecl";
    }
}
