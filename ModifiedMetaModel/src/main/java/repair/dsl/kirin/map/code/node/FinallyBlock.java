package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class FinallyBlock extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "finallyBlock";
    }

    @Override
    public String getAlias() {
        return "finalBlk";
    }
}
