package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class CastExpression extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "castExpression";
    }

    @Override
    public String getAlias() {
        return "castExp";
    }
}
