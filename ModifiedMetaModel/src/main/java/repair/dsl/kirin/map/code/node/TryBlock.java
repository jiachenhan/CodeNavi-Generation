package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class TryBlock extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "tryBlock";
    }

    @Override
    public String getAlias() {
        return "tryBlk";
    }
}
