package repair.dsl.kirin.condition;

import repair.dsl.kirin.alias.Alias;

public class BoolCondition extends Condition {
    private final Alias alias;
    private final String conditionStr;

    public BoolCondition(Alias alias, String conditionStr) {
        this.alias = alias;
        this.conditionStr = conditionStr;
    }

    @Override
    public String prettyPrint() {
        return alias.getAliasKey() + "." + conditionStr;
    }
}
