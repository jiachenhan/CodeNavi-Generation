package repair.modify.builder;

import repair.ast.MoNode;
import repair.ast.role.Description;

import java.io.Serial;
import java.io.Serializable;

@Deprecated
public class MoSimpleVirtualNode<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1930742776005621544L;

    private final T simpleChild;
    private final MoNode parent;
    private final Description<? extends MoNode, ?> location;

    public MoSimpleVirtualNode(MoNode parent, T simpleChild, Description<? extends MoNode, ?> location) {
        this.parent = parent;
        this.simpleChild = simpleChild;
        this.location = location;
    }

    public MoNode getParent() {
        return parent;
    }

    public T getSimpleChild() {
        return simpleChild;
    }

    public Description<? extends MoNode, ?> getLocation() {
        return location;
    }
}
