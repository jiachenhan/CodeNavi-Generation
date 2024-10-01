package repair.ast.role;

import repair.ast.MoNode;

import java.io.Serial;
import java.io.Serializable;

/**
 * @param classification child, childList, simple
 */
public record Description<U extends MoNode, V>(ChildType classification, Class<U> parentNodeType,
                                               Class<V> childNodeType, String role, boolean mandatory) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
