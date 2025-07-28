package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class ExceptionBlock extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "exceptionBlock";
    }

    @Override
    public String getAlias() {
        return "exceptBlk";
    }
}
