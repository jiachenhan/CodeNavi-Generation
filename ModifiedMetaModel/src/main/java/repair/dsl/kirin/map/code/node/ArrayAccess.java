package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class ArrayAccess extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "arrayAccess";
    }

    @Override
    public String getAlias() {
        return "arrayAcc";
    }
}
