package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class AssignStatement extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "binaryOperation";
    }

    @Override
    public String getAlias() {
        return "assign";
    }
}
