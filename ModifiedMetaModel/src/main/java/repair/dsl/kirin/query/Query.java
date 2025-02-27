package repair.dsl.kirin.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.dsl.kirin.Printable;
import repair.dsl.kirin.alias.Alias;
import repair.dsl.kirin.alias.AliasManager;
import repair.dsl.kirin.alias.Aliasable;
import repair.dsl.kirin.condition.BinaryCondition;
import repair.dsl.kirin.condition.Condition;
import repair.dsl.kirin.condition.UnionCondition;
import repair.dsl.kirin.expr.Rhs;

import java.util.Optional;

public abstract class Query implements Printable, Aliasable, Rhs {
    private static final Logger logger = LoggerFactory.getLogger(Query.class);

    protected static final String conditionPrefix = "where";
    protected final AliasManager aliasManager;
    protected Condition condition;

    private final MoNode referenceNode;

    public Query(MoNode referenceNode) {
        this.referenceNode = referenceNode;
        this.aliasManager = AliasManager.getInstance();
    }

    @Override
    public Alias getAlias() {
        return aliasManager.getAlias(this);
    }

    public void addCondition(Condition condition) {
        if (this.condition == null) {
            setCondition(condition);
        } else if (this.condition instanceof UnionCondition unionCondition
                && unionCondition.getPredicate() == UnionCondition.Predicate.AND) {
            if (unionCondition.isDuplicate(condition)) {
                logger.debug("Duplicate condition: {}", condition.prettyPrint());
                return;
            }
            unionCondition.addInnerCondition(condition);
        } else {
            UnionCondition unionCondition = new UnionCondition(UnionCondition.Predicate.AND);
            unionCondition.addInnerCondition(this.condition);
            if (unionCondition.isDuplicate(condition)) {
                logger.debug("Duplicate condition: {}", condition.prettyPrint());
                return;
            }
            unionCondition.addInnerCondition(condition);
            setCondition(unionCondition);
        }
    }

    public MoNode getReferenceNode() {
        return referenceNode;
    }

    public void setCondition(Condition condition) {
        this.condition = condition;
    }

    public Optional<Condition> getCondition() {
        return Optional.ofNullable(condition);
    }
}
