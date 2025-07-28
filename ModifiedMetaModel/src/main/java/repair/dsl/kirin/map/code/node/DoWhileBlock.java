package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class DoWhileBlock extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "doWhileBlock";
    }

    @Override
    public String getAlias() {
        return "doWhileBlk";
    }
}
