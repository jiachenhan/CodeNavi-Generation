package repair.modify.diff.operations;

import com.github.gumtreediff.actions.model.Delete;
import repair.ast.MoNode;
import repair.modify.builder.GumtreeMetaConstant;

public class DeleteOperation extends Operation<Delete> {
    // in before tree
    private final MoNode deleteNode;

    public DeleteOperation(Delete delete) {
        super(delete);
        this.deleteNode = (MoNode) action.getNode().getMetadata(GumtreeMetaConstant.MO_NODE_KEY);
    }

    public MoNode getDeleteNode() {
        return deleteNode;
    }


}
