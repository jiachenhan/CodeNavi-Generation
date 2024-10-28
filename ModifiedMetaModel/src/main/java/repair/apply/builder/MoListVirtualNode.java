package repair.apply.builder;

import repair.ast.MoNodeList;

import java.io.Serial;
import java.io.Serializable;

/**
 *  this class is used to represent a virtual node that has a list of children
 * @param <T> is the type of the children
 */
@Deprecated
public class MoListVirtualNode<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = -7979471324420376846L;

    private final MoNodeList<T> children;

    public MoListVirtualNode(MoNodeList<T> moNodeList) {
        this.children = moNodeList;
    }

    public MoNodeList<T> getChildren() {
        return children;
    }
}
