package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class ContinueStatement extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "continueStatement";
    }
}
