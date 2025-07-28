package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class TernaryOperation extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "ternaryOperation";
    }

    @Override
    public String getAlias() {
        return "ternOper";
    }
}
