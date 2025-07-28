package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class InstanceofExpression extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "instanceofExpression";
    }

    @Override
    public String getAlias() {
        return "insExpr";
    }
}
