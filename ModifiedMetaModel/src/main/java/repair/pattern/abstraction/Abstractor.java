package repair.pattern.abstraction;

import org.apache.commons.collections4.BidiMap;
import repair.ast.MoNode;
import repair.apply.diff.operations.*;
import repair.pattern.Pattern;
import repair.pattern.attr.Attribute;

import java.util.ArrayList;
import java.util.List;

public interface Abstractor {
    boolean shouldConsider(MoNode node);
    boolean shouldConsider(Attribute<?> attribute);
    void doAbstraction(Pattern pattern);

    default List<MoNode> getActionRelatedNodes(Pattern pattern){
        List<MoNode> nodes = new ArrayList<>();
        BidiMap<MoNode, MoNode> beforeToAfterMap = pattern.getBeforeToAfterMap();
        pattern.getAllOperations().forEach(action -> {
            if(action instanceof DeleteOperation deleteOperation) {
                MoNode deleteNodeInBefore = deleteOperation.getDeleteNode();
                nodes.add(deleteNodeInBefore);
            } else if(action instanceof TreeDeleteOperation deleteOperation) {
                MoNode deleteNodeInBefore = deleteOperation.getDeleteNodeInBefore();
                nodes.add(deleteNodeInBefore);
            } else if(action instanceof InsertOperation insertOperation) {
                MoNode insertParent = insertOperation.getParent();
                if(beforeToAfterMap.containsKey(insertParent)) {
                    nodes.add(insertParent);
                } else if (beforeToAfterMap.containsValue(insertParent)) {
                    MoNode insertParentBefore = beforeToAfterMap.getKey(insertParent);
                    nodes.add(insertParentBefore);
                }
            } else if (action instanceof TreeInsertOperation treeInsertOperation) {
                MoNode insertParent = treeInsertOperation.getParent();
                if(beforeToAfterMap.containsKey(insertParent)) {
                    nodes.add(insertParent);
                } else if (beforeToAfterMap.containsValue(insertParent)) {
                    MoNode insertParentBefore = beforeToAfterMap.getKey(insertParent);
                    nodes.add(insertParentBefore);
                }
            } else if (action instanceof MoveOperation moveOperation) {
                MoNode moveParent = moveOperation.getParent();
                if(beforeToAfterMap.containsKey(moveParent)) {
                    nodes.add(moveParent);
                } else if (beforeToAfterMap.containsValue(moveParent)) {
                    MoNode moveParentBefore = beforeToAfterMap.getKey(moveParent);
                    nodes.add(moveParentBefore);
                }

                MoNode moveNodeInBefore = moveOperation.getMoveNode();
                nodes.add(moveNodeInBefore);
            } else if(action instanceof UpdateOperation updateOperation) {
                MoNode updateNodeInBefore = updateOperation.getUpdateNode();
                nodes.add(updateNodeInBefore);
            } else {
                throw new RuntimeException("Unknown action type");
            }
        });
        return nodes;
    }
}
