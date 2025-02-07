package repair.pattern;

import repair.ast.MoNode;
import repair.ast.role.Description;

import java.io.Serializable;
import java.util.Map;

public record MoveNode(MoNode moveNode, MoNode moveParent, Description<?, ?> moveLocation, Map<MoNode, Boolean> moveParentConsideredNode) implements Serializable {
}
