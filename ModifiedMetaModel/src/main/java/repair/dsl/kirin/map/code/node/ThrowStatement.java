package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class ThrowStatement extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "throwStatement";
    }

    @Override
    public String getAlias() {
        return "throw";
    }
}
