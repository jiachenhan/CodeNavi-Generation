package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class ThisExpression extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "thisExpression";
    }

    @Override
    public String getAlias() {
        return "thisExpr";
    }
}
