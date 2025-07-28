package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class AnonymousInnerClassExpression extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "anonymousInnerClassExpression";
    }

    @Override
    public String getAlias() {
        return "innerClassExp";
    }
}
