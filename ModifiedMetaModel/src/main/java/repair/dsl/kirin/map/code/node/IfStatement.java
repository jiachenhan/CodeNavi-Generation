package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class IfStatement extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "ifBlock";
    }

    @Override
    public String getAlias() {
        return "ifBlk";
    }
}
