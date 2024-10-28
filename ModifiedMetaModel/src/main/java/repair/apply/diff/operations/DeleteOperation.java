package repair.apply.diff.operations;

import com.github.gumtreediff.actions.model.Delete;
import repair.ast.MoNode;
import repair.apply.builder.GumtreeMetaConstant;

import java.io.Serial;

public class DeleteOperation extends Operation<Delete> {
    @Serial
    private static final long serialVersionUID = -4140232363719773973L;
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
