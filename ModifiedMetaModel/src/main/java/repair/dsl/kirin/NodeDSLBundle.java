package repair.dsl.kirin;

import repair.ast.MoNode;
import repair.dsl.kirin.map.code.node.DSLNode;

public class NodeDSLBundle {
    private final boolean isConsider;
    private final MoNode originalNode;
    private final DSLNode dslNode;

    public NodeDSLBundle(boolean isConsider, MoNode originalNode, DSLNode dslNode) {
        this.isConsider = isConsider;
        this.originalNode = originalNode;
        this.dslNode = dslNode;
    }

    public boolean isConsider() {
        return isConsider;
    }

    public MoNode getOriginalNode() {
        return originalNode;
    }

    public DSLNode getDslNode() {
        return dslNode;
    }
}
