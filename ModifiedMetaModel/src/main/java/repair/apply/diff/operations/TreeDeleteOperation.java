package repair.apply.diff.operations;

import com.github.gumtreediff.actions.model.TreeDelete;
import repair.ast.MoNode;
import repair.apply.builder.GumtreeMetaConstant;

import java.io.Serial;

public class TreeDeleteOperation extends Operation<TreeDelete>{
    @Serial
    private static final long serialVersionUID = -4992782168004525051L;
    private final MoNode deleteNodeInBefore;

    public TreeDeleteOperation(TreeDelete action) {
        super(action);
        this.deleteNodeInBefore = (MoNode) action.getNode().getMetadata(GumtreeMetaConstant.MO_NODE_KEY);
    }

    public MoNode getDeleteNodeInBefore() {
        return deleteNodeInBefore;
    }

}
