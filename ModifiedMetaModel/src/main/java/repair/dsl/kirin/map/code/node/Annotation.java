package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class Annotation extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "annotation";
    }

    @Override
    public String getAlias() {
        return "anno";
    }
}
