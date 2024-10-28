package repair.apply.diff.operations;

import com.github.gumtreediff.actions.model.TreeInsert;
import repair.ast.MoNode;
import repair.ast.role.Description;
import repair.apply.builder.GumtreeMetaConstant;

import java.io.Serial;

public class TreeInsertOperation extends Operation<TreeInsert> implements AddOperator {
    @Serial
    private static final long serialVersionUID = 5013163995056290045L;
    private final MoNode insertParent;
    private final Description<? extends MoNode, ?> insertLocation;
    private final MoNode inserteeNodeInAfter;
    private final InsertListStrategy strategy;

    public TreeInsertOperation(TreeInsert action) {
        super(action);
        inserteeNodeInAfter = (MoNode) action.getNode().getMetadata(GumtreeMetaConstant.MO_NODE_KEY);
        insertLocation = inserteeNodeInAfter.getLocationInParent();
        insertParent = (MoNode) action.getParent().getMetadata(GumtreeMetaConstant.MO_NODE_KEY);

        strategy = new OriginGumtreeInsertStrategy(action.getPosition());
//        strategy = new NaiveIndexStrategy(this);
    }

    @Override
    public MoNode getAddNode() {
        return inserteeNodeInAfter;
    }

    @Override
    public Description<? extends MoNode, ?> getLocation() {
        return insertLocation;
    }

    @Override
    public MoNode getParent() {
        return insertParent;
    }

    @Override
    public int computeIndex() {
        return strategy.computeInsertIndex();
    }

}
