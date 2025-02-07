package repair.dsl.kirin.query;

import repair.ast.MoNode;

public class AliasQuery extends Query {
    public AliasQuery(MoNode referenceNode) {
        super(referenceNode);
    }

    @Override
    public String prettyPrint() {
        if (getCondition().isEmpty()) {
            return "";
        }
        return getAlias().getAliasKey() +
                " " + conditionPrefix + " " + getCondition().get().prettyPrint();
    }
}
