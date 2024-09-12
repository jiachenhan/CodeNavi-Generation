package repair.ast.role;

import repair.ast.MoNode;

/**
 * @param classification child, childList, simple
 */
public record Description<U extends MoNode, V>(ChildType classification, Class<U> parentNodeType,
                                               Class<V> childNodeType, String role, boolean mandatory) {
}
