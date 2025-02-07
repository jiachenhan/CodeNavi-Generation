package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class CatchBlock extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "catchBlock";
    }
}
