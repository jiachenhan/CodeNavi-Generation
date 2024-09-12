package repair.ast.behavior;

import repair.ast.MoNode;

public interface NodeComparator {
    boolean isSame(MoNode other);
}
