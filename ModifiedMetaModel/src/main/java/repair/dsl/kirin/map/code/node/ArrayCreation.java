package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class ArrayCreation extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "arrayCreationExpression";
    }
}
