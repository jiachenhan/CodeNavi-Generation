package repair.dsl.kirin.query;

import repair.ast.MoNode;
import repair.dsl.kirin.map.code.node.DSLNode;

public class NormalQuery extends Query {
    protected final DSLNode dslNode;

    public NormalQuery(MoNode referenceNode, DSLNode dslNode) {
        super(referenceNode);
        this.dslNode = dslNode;
    }

    public DSLNode getDslNode() {
        return dslNode;
    }

    @Override
    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        sb.append(dslNode.prettyPrint()).append(" ").append(getAlias().getAliasKey());
        if (getCondition().isPresent()) {
            sb.append(" ").append(conditionPrefix).append(" ").append(getCondition().get().prettyPrint());
        }
        return sb.toString();
    }
}
