package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class NullLiteral extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "nullLiteral";
    }

    @Override
    public String getAlias() {
        return "nullLit";
    }
}
