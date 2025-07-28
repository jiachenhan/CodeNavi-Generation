package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class WhileStatement extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "whileBlock";
    }

    @Override
    public String getAlias() {
        return "whileBlk";
    }
}
