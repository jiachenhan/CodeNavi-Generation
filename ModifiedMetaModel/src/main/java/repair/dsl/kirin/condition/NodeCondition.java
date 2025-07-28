package repair.dsl.kirin.condition;

import repair.dsl.kirin.alias.Alias;
import repair.dsl.kirin.alias.QueryAliasable;

public class NodeCondition extends Condition implements QueryAliasable {
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
