package repair.pattern;

import repair.ast.MoNode;
import repair.ast.role.Description;

import java.io.Serializable;
import java.util.Map;

public record InsertNode(MoNode insertNode, MoNode insertParent, Description<?, ?> insertLocation, Map<MoNode, Boolean> insertConsideredNode) implements Serializable {
}
