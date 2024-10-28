package repair.apply.diff.operations;

import repair.ast.MoNode;
import repair.ast.role.Description;

public interface AddOperator {
    MoNode getAddNode();

    MoNode getParent();

    Description<? extends MoNode, ?> getLocation();

    int computeIndex();
}
