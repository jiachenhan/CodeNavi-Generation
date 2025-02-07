package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class FunctionCall extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "functionCall";
    }
}
