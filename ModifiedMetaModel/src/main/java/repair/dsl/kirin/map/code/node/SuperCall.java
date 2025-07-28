package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class SuperCall extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "superCall";
    }

    @Override
    public String getAlias() {
        return "superCall";
    }
}
