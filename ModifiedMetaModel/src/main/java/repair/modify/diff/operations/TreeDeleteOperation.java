package repair.modify.diff.operations;

import com.github.gumtreediff.actions.model.TreeDelete;
import repair.ast.MoNode;
import repair.modify.builder.GumtreeMetaConstant;

public class TreeDeleteOperation extends Operation<TreeDelete>{

    private final MoNode deleteNodeInBefore;

    public TreeDeleteOperation(TreeDelete action) {
        super(action);
        this.deleteNodeInBefore = (MoNode) action.getNode().getMetadata(GumtreeMetaConstant.MO_NODE_KEY);
    }

    public MoNode getDeleteNodeInBefore() {
        return deleteNodeInBefore;
    }

}
