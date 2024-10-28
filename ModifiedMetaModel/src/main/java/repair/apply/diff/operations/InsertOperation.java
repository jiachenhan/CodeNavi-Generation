package repair.apply.diff.operations;

import com.github.gumtreediff.actions.model.Insert;
import repair.ast.MoNode;
import repair.ast.role.Description;
import repair.apply.builder.GumtreeMetaConstant;

import java.io.Serial;

public class InsertOperation extends Operation<Insert> implements AddOperator {
    @Serial
    private static final long serialVersionUID = -4752124810495063795L;
    private final Description<? extends MoNode, ?> insertLocation;
    // 可能在两个树中存在
    private final MoNode insertParent;
    // in after tree
    private final MoNode insertNode;

    private final InsertListStrategy strategy;
    public InsertOperation(Insert action) {
        super(action);
        insertParent = (MoNode) action.getParent().getMetadata(GumtreeMetaConstant.MO_NODE_KEY);

        insertNode = (MoNode) action.getNode().getMetadata(GumtreeMetaConstant.MO_NODE_KEY);
        insertLocation = insertNode.getLocationInParent();

        strategy = new OriginGumtreeInsertStrategy(action.getPosition());
//        strategy = new NaiveIndexStrategy(this);
    }

    @Override
    public MoNode getAddNode() {
        return insertNode;
    }

    @Override
    public MoNode getParent() {
        return insertParent;
    }
    @Override
    public Description<? extends MoNode, ?> getLocation() {
        return insertLocation;
    }

    @Override
    public int computeIndex() {
        return strategy.computeInsertIndex();
    }
}
