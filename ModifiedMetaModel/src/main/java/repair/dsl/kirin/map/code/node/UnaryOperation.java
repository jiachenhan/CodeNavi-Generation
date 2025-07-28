package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class UnaryOperation extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "unaryOperation";
    }

    @Override
    public String getAlias() {
        return "unaryOper";
    }
}
