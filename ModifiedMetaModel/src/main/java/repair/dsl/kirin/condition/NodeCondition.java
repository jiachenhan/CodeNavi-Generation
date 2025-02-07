package repair.dsl.kirin.condition;

import repair.dsl.kirin.alias.Alias;
import repair.dsl.kirin.alias.Aliasable;

public class NodeCondition extends Condition implements Aliasable {
    // 先不考虑

    @Override
    public String prettyPrint() {
        return "";
    }

    @Override
    public Alias getAlias() {
        return null;
    }
}
