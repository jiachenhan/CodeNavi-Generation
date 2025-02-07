package repair.pattern;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import repair.apply.builder.GumtreeMetaConstant;
import repair.ast.MoNode;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NotLogicManager implements Serializable {
    @Serial
    private static final long serialVersionUID = -9006194862530860311L;

    private final Pattern pattern;

    private final BidiMap<MoNode, MoNode> beforeToAfterMap = new DualHashBidiMap<>();

    public NotLogicManager(Pattern pattern) {
        this.pattern = pattern;
        pattern.getDiffComparator().getMappings().asSet().forEach(mapping -> {
            MoNode beforeNode = (MoNode) mapping.first.getMetadata(GumtreeMetaConstant.MO_NODE_KEY);
            MoNode afterNode = (MoNode) mapping.second.getMetadata(GumtreeMetaConstant.MO_NODE_KEY);
            beforeToAfterMap.put(beforeNode, afterNode);
        });

        gainInsertNodes();
        gainMoveNodes();
    }

    private final List<MoveNode> moveNodes = new ArrayList<>();
    private final List<InsertNode> insertNodes = new ArrayList<>();

    private void gainMoveNodes() {
        // todo:
    }

    private void gainInsertNodes() {

    }

    public List<InsertNode> getInsertNodes() {
        return insertNodes;
    }

    public List<MoveNode> getMoveNodes() {
        return moveNodes;
    }
}
