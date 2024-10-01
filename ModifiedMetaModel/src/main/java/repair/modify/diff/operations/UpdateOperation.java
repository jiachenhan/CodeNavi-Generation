package repair.modify.diff.operations;

import com.github.gumtreediff.actions.model.Update;
import repair.ast.MoNode;
import repair.modify.builder.GumtreeMetaConstant;

import java.io.Serial;

public class UpdateOperation extends Operation<Update> {
    @Serial
    private static final long serialVersionUID = 1353845367393661509L;
    // in before tree
    private final MoNode updateNode;
    private final String updateValue;
    public UpdateOperation(Update action) {
        super(action);
        this.updateNode = (MoNode) action.getNode().getMetadata(GumtreeMetaConstant.MO_NODE_KEY);
        this.updateValue = action.getValue();
    }

    public MoNode getUpdateNode() {
        return updateNode;
    }

    public String getUpdateValue() {
        return updateValue;
    }
}
