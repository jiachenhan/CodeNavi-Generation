package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class BreakStatement extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "breakStatement";
    }

    @Override
    public String getAlias() {
        return "break";
    }
}
