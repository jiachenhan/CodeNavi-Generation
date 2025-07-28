package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class CaseStatement extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "caseStatement";
    }

    @Override
    public String getAlias() {
        return "case";
    }
}
