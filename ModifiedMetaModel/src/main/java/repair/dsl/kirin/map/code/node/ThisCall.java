package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class ThisCall extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "thisCall";
    }
}
