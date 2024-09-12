package repair.ast.code;

import repair.ast.MoNode;

import java.util.List;

/**
 * now support:
 *     TagElement
 *     TextElement
 *     Name
 *        SimpleName
 *        QualifiedName
 */
public interface MoDocElement {
    // marker-type interfaces have no members

    public static boolean sameList(List<MoDocElement> list1, List<MoDocElement> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }
        for (int i = 0; i < list1.size(); i++) {
            MoNode node1 = (MoNode) list1.get(i);
            MoNode node2 = (MoNode) list2.get(i);
            if (!node1.isSame(node2)) {
                return false;
            }
        }
        return true;
    }
}
