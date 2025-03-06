package repair.pattern;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import repair.apply.builder.GumtreeMetaConstant;
import repair.apply.diff.operations.InsertOperation;
import repair.apply.diff.operations.MoveOperation;
import repair.apply.diff.operations.TreeInsertOperation;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.statement.MoBlock;
import repair.ast.code.statement.MoExpressionStatement;
import repair.ast.code.statement.MoTryStatement;
import repair.ast.role.Description;
import repair.ast.visitor.FlattenScanner;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        if (insertNodes.size() > 10) {
            insertNodes.clear();
        }
        if (moveNodes.size() > 10) {
            moveNodes.clear();
        }
    }

    private final List<MoveNode> moveNodes = new ArrayList<>();
    private final List<InsertNode> insertNodes = new ArrayList<>();

    private void gainMoveNodes() {
        List<MoveOperation> moveOps = pattern.getAllOperations().stream()
                .filter(operation -> operation instanceof MoveOperation)
                .map(operation -> (MoveOperation) operation)
                .toList();

        for (MoveOperation moveOp : moveOps) {
            if (isStructureNode(moveOp.getMoveParent())) {
                if (moveOp.getMoveNode().getParent() != moveOp.getMoveParent()) {
                    MoNode moveNode = moveOp.getMoveNode();
                    MoNode moveParent = moveOp.getMoveParent();
                    Description<? extends MoNode, ?> moveLocation = moveOp.getLocation();

                    if (moveParent instanceof MoBlock) {
                        if (! (moveParent.getParent() instanceof MoTryStatement)) {
                            moveParent = moveParent.getParent();
                        }
                    }
                    MoNode finalParent = moveParent;

                    Map<MoNode, Boolean> moveParentConsideredNode = new HashMap<>();
                    for (MoNode beforeNode : new FlattenScanner().flatten(finalParent)) {
                        // 排除掉新insert进来占位的和moveNode相同的节点
                        List<MoNode> moveElement = new FlattenScanner().flatten(moveNode);
                        if (! hasSameElement(beforeNode, moveElement)) {
                            moveParentConsideredNode.put(beforeNode, true);
                        }
                    }

                    // 去掉中间Skip层，包括block，expressionStmt等
                    if (moveNode instanceof MoBlock block) {
                        block.getStatements().forEach(moveStmt -> {
                            if (moveStmt instanceof MoExpressionStatement expressionStatement) {
                                moveNodes.add(new MoveNode(expressionStatement.getExpression(), finalParent, moveLocation, moveParentConsideredNode));
                            } else {
                                moveNodes.add(new MoveNode(moveStmt, finalParent, moveLocation, moveParentConsideredNode));
                            }
                        });
                    } else {
                        if (moveNode instanceof MoExpressionStatement expressionStatement) {
                            moveNodes.add(new MoveNode(expressionStatement.getExpression(), finalParent, moveLocation, moveParentConsideredNode));
                        } else {
                            moveNodes.add(new MoveNode(moveNode, finalParent, moveLocation, moveParentConsideredNode));
                        }
                    }
                }
            }
        }
    }

    private void gainInsertNodes() {
        List<TreeInsertOperation> treeInsertOps = pattern.getAllOperations().stream()
                .filter(operation -> operation instanceof TreeInsertOperation)
                .map(operation -> (TreeInsertOperation) operation)
                .toList();

        List<InsertOperation> insertOps = pattern.getAllOperations().stream()
                .filter(operation -> operation instanceof InsertOperation)
                .map(operation -> (InsertOperation) operation)
                .toList();


        // insert parent 在 before tree 中 （apply中的type1）
        for (TreeInsertOperation treeInsertOp : treeInsertOps) {
            if (beforeToAfterMap.containsKey(treeInsertOp.getParent())) {
                MoNode insertNode = treeInsertOp.getAddNode();
                MoNode insertParent = treeInsertOp.getParent();

                // 如果插入节点的parent是move节点的话，那么他不在最终正确的位置上，直接忽略这种insert
                if (insertInMovedNode(insertParent)) {
                    continue;
                }
                // 如果插入的节点是move的parent，那么对应的逻辑直接在notin中体现
                if (insertInMoveParent(insertNode)) {
                    continue;
                }

                Description<? extends MoNode, ?> insertLocation = treeInsertOp.getLocation();
                if (insertNode instanceof MoBlock moBlock) {
                    moBlock.getStatements().forEach(insertStmt -> {
                        if (insertStmt instanceof MoExpressionStatement expressionStatement) {
                            Map<MoNode, Boolean> insertConsideredNode = new HashMap<>();
                            for (MoNode beforeNode : new FlattenScanner().flatten(expressionStatement.getExpression())) {
                                insertConsideredNode.put(beforeNode, true);
                            }
                            insertNodes.add(new InsertNode(expressionStatement.getExpression(), insertParent, insertLocation, insertConsideredNode));
                        } else {
                            Map<MoNode, Boolean> insertConsideredNode = new HashMap<>();
                            for (MoNode beforeNode : new FlattenScanner().flatten(insertStmt)) {
                                insertConsideredNode.put(beforeNode, true);
                            }
                            insertNodes.add(new InsertNode(insertStmt, insertParent, insertLocation, insertConsideredNode));
                        }
                    });
                } else {
                    if (insertNode instanceof MoExpressionStatement expressionStatement) {
                        Map<MoNode, Boolean> insertConsideredNode = new HashMap<>();
                        for (MoNode beforeNode : new FlattenScanner().flatten(expressionStatement.getExpression())) {
                            insertConsideredNode.put(beforeNode, true);
                        }
                        insertNodes.add(new InsertNode(expressionStatement.getExpression(), insertParent, insertLocation, insertConsideredNode));
                    } else {
                        Map<MoNode, Boolean> insertConsideredNode = new HashMap<>();
                        for (MoNode beforeNode : new FlattenScanner().flatten(insertNode)) {
                            insertConsideredNode.put(beforeNode, true);
                        }
                        insertNodes.add(new InsertNode(insertNode, insertParent, insertLocation, insertConsideredNode));
                    }
                }

            }
        }

        for (InsertOperation insertOp : insertOps) {
            MoNode insertNode = insertOp.getAddNode();
            MoNode insertParent = insertOp.getParent();

            // 如果插入的节点是move的parent，那么对应的逻辑直接在notin中体现
            if (insertInMoveParent(insertNode)) {
                continue;
            }

            Description<? extends MoNode, ?> insertLocation = insertOp.getLocation();
            if (insertNode instanceof MoExpressionStatement expressionStatement) {
                Map<MoNode, Boolean> insertConsideredNode = new HashMap<>();
                for (MoNode beforeNode : new FlattenScanner().flatten(expressionStatement.getExpression())) {
                    insertConsideredNode.put(beforeNode, true);
                }
                insertNodes.add(new InsertNode(expressionStatement.getExpression(), insertParent, insertLocation, insertConsideredNode));
            } else {
                Map<MoNode, Boolean> insertConsideredNode = new HashMap<>();
                for(MoNode beforeNode : new FlattenScanner().flatten(insertNode)) {
                    insertConsideredNode.put(beforeNode, true);
                }
                insertNodes.add(new InsertNode(insertNode, insertParent, insertLocation, insertConsideredNode));
            }
        }

    }

    private boolean insertInMoveParent(MoNode insertParent) {
        List<MoNode> movedParent = moveNodes.stream().map(MoveNode::moveParent).toList();
        return movedParent.contains(insertParent);
    }

    private boolean insertInMovedNode(MoNode insertParent) {
        List<MoNode> movedNodes = pattern.getAllOperations().stream()
                .filter(operation -> operation instanceof MoveOperation)
                .map(operation -> (MoveOperation) operation)
                .map(MoveOperation::getMoveNode)
                .toList();

        return movedNodes.contains(insertParent);
    }

    private boolean isStructureNode(MoNode node) {
        List<MoNodeType> structureTypes = List.of(MoNodeType.TYPEIfStatement, MoNodeType.TYPETryStatement,
                MoNodeType.TYPEForStatement, MoNodeType.TYPEWhileStatement, MoNodeType.TYPECatchClause,
                MoNodeType.TYPEEnhancedForStatement, MoNodeType.TYPESynchronizedStatement, MoNodeType.TYPEBlock,
                MoNodeType.TYPEDoStatement, MoNodeType.TYPESwitchStatement);

        return structureTypes.contains(node.getMoNodeType());
    }

    private boolean hasSameElement(MoNode child, List<MoNode> nodeList) {
        for (MoNode moNode : nodeList) {
            if (child.isSame(moNode)) {
                return true;
            }
        }
        return false;
    }

    public List<InsertNode> getInsertNodes() {
        return insertNodes;
    }

    public List<MoveNode> getMoveNodes() {
        return moveNodes;
    }
}
