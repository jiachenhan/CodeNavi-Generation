package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.map.code.KeyWord;

public class DefaultCase extends DSLNode implements KeyWord {
    @Override
    public String prettyPrint() {
        return "defaultStatement";
    }

    @Override
    public String getAlias() {
        return "defaultStat";
    }
}
