package repair.modify.diff.operations;

import com.github.gumtreediff.actions.model.Move;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.utils.Pair;
import repair.ast.MoNode;
import repair.ast.role.Description;
import repair.modify.builder.GumtreeMetaConstant;

public class MoveOperation extends Operation<Move> implements AddOperator {
    // in before tree
    private final MoNode moveNode;
    // in after tree
    private final MoNode moveParent;

    private Pair<MoNode, MoNode> movePair;

    private Description<? extends MoNode, ?> moveToLocation;
    private final InsertListStrategy strategy;

    public MoveOperation(Move action, MappingStore mappings) {
        super(action);
        moveParent = ((MoNode) action.getParent().getMetadata(GumtreeMetaConstant.MO_NODE_KEY));
        moveNode = ((MoNode) action.getNode().getMetadata(GumtreeMetaConstant.MO_NODE_KEY));

        Tree moveDst = mappings.getDstForSrc(action.getNode());
        if (moveDst != null) {
            MoNode moveDstNode = (MoNode) moveDst.getMetadata(GumtreeMetaConstant.MO_NODE_KEY);
            moveToLocation = moveDstNode.getLocationInParent();
            movePair = new Pair<>(moveNode, moveDstNode);
        }

        strategy = new OriginGumtreeInsertStrategy(action.getPosition());
    }

    public MoNode getMoveNode() {
        return moveNode;
    }

    public MoNode getMoveParent() {
        return moveParent;
    }

    @Override
    public MoNode getAddNode() {
        return movePair.second;
    }

    @Override
    public MoNode getParent() {
        return moveParent;
    }

    @Override
    public Description<? extends MoNode, ?> getLocation() {
        return moveToLocation;
    }

    @Override
    public int computeIndex() {
        return strategy.computeInsertIndex();
    }
}
