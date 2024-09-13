package repair.apply;

import com.github.gumtreediff.actions.model.Action;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.apply.match.MatchInstance;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.code.MoModifier;
import repair.ast.code.expression.MoMethodInvocation;
import repair.ast.code.expression.MoSimpleName;
import repair.ast.code.expression.literal.MoBooleanLiteral;
import repair.ast.code.expression.literal.MoCharacterLiteral;
import repair.ast.code.expression.literal.MoNumberLiteral;
import repair.ast.code.expression.literal.MoStringLiteral;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.visitor.DeepCopyScanner;
import repair.modify.builder.GumtreeMetaConstant;
import repair.modify.diff.operations.*;
import repair.pattern.Pattern;

/**
 *  the big picture of applying modification process
 *
 *  maintain two couple of trees
 *  before <---> after (in pattern)
 *  left <---> right (code and its copy for modification)
 *
 *  1. find the mapping between before and left (based on token similarity matching)
 *  2. copy the left tree to right tree
 *  3. apply the modification to the right tree and maintain the mapping between after and right (for insertion on after tree)
 */
public class ApplyModification {
    private static final Logger logger = LoggerFactory.getLogger(ApplyModification.class);
    private final Pattern pattern;
    private final MoNode left;
    private MoNode right;


    /**
     * before <---> after mapping (based on mapping store)
     */
    private final BidiMap<MoNode, MoNode> beforeToAfterMap = new DualHashBidiMap<>();
    /**
     * before <---> left mapping (based on token similarity)
     */
    private final MatchInstance matchInstance;
    /**
     * left <---> right mapping (based on copying)
     */
    private BidiMap<MoNode, MoNode> leftToRightMap;

    /**
     * after <---> right mapping (based on applying Operation, from copy)
     */
    private final BidiMap<MoNode, MoNode> maintenanceMap = new DualHashBidiMap<>();

    public ApplyModification(Pattern pattern, MoNode left, MatchInstance matchInstance) {
        this.pattern = pattern;

        this.left = left;
        DeepCopyScanner deepCopyScanner = new DeepCopyScanner(left);
        this.right = deepCopyScanner.getCopy();
        this.leftToRightMap = deepCopyScanner.getCopyMap();

        pattern.getDiffComparator().getMappings().asSet().forEach(mapping -> {
            MoNode beforeNode = (MoNode) mapping.first.getMetadata(GumtreeMetaConstant.MO_NODE_KEY);
            MoNode afterNode = (MoNode) mapping.second.getMetadata(GumtreeMetaConstant.MO_NODE_KEY);
            beforeToAfterMap.put(beforeNode, afterNode);
        });

        this.matchInstance = matchInstance;
    }

    public MoNode getRight() {
        return right;
    }

    public void apply() {
        for (Operation<? extends Action> operation : pattern.getAllOperations()) {
            if (operation instanceof DeleteOperation deleteOperation) {
                MoNode deleteNodeInBefore = deleteOperation.getDeleteNode();
                MoNode deleteNodeInLeft = this.matchInstance.getNodeMap().get(deleteNodeInBefore);
                if(deleteNodeInLeft == null) {
                    logger.error("can not find the delete node in left tree, matching error");
                    return;
                }
                MoNode deleteNodeInRight = this.leftToRightMap.get(deleteNodeInLeft);
                assert deleteNodeInRight != null;
                deleteNodeInRight.removeFromParent();
            } else if(operation instanceof InsertOperation insertOperation) {
                MoNode insertParent = insertOperation.getParent();
                Description<? extends MoNode, ?> insertLocation = insertOperation.getLocation();

                // find the insertParent in right
                // 对于insert操作有三种情况
                // 1. insertParent在before中，这种情况出现于插入的节点插入到List中，产生新的结构
                // 2. insertParent在After中，但是在before中有对应的节点 ，这种情况出现于插入元素对原本位置元素的替换
                // 3. insertParent在before中没有对应的节点，但是在之前的操作中已经插入到right中，这种情况需要从maintenanceMap中找到对应的节点
                MoNode insertParentInRight = null;
                if(this.beforeToAfterMap.containsKey(insertParent)) {
                    logger.info("insertParent type 1");
                    MoNode insertParentType1Left = matchInstance.getNodeMap().get(insertParent);
                    if(insertParentType1Left == null) {
                        logger.error("error when Insert because insertParentType1Left is null, matching error");
                        return;
                    }
                    insertParentInRight = this.leftToRightMap.get(insertParentType1Left);
                } else {
                    MoNode insertParentType2Before = this.beforeToAfterMap.getKey(insertParent);
                    if(insertParentType2Before != null) {
                        logger.info("insertParent type 2");
                        MoNode insertParentType2Left = this.matchInstance.getNodeMap().get(insertParentType2Before);
                        if(insertParentType2Left == null) {
                            logger.error("error when Insert because insertParentType2Left is null, matching error");
                            return;
                        }
                        insertParentInRight = this.leftToRightMap.get(insertParentType2Left);
                    } else {
                        logger.info("insertParent type 3");
                        MoNode insertParentType3 = maintenanceMap.get(insertParent);
                        if(insertParentType3 == null) {
                            logger.error("error when Insert because insertParent is not in before tree and maintenanceMap");
                            return;
                        }
                        insertParentInRight = insertParentType3;
                    }
                }
                assert insertParentInRight != null;


                // generate the insertee node in right
                MoNode insertNodeTemplate = insertOperation.getAddNode();
                MoNode insertNodeInRight = insertNodeTemplate.shallowClone();
                maintenanceMap.put(insertNodeTemplate, insertNodeInRight);

                // insert the insertee node in right
                if(insertLocation.classification() == ChildType.CHILDLIST) {
                    MoNodeList<MoNode> children = (MoNodeList<MoNode>) insertParentInRight.getStructuralProperty(insertLocation.role());
                    int index = insertOperation.computeIndex();
                    if(index < 0 || index > children.size()) {
                        logger.error("error when Insert because index is out of bound");
                        return;
                    }

                    children.add(index, insertNodeInRight);
                } else if (insertLocation.classification() == ChildType.CHILD) {
                    insertParentInRight.setStructuralProperty(insertLocation.role(), insertNodeInRight);
                } else {
                    logger.error("error when Insert because insertLocation is single");
                }
            } else if(operation instanceof MoveOperation moveOperation) {
                MoNode moveNodeInBefore = moveOperation.getMoveNode();
                MoNode moveParent = moveOperation.getMoveParent();
                Description<? extends MoNode, ?> moveToLocation = moveOperation.getLocation();

                // find the moveParent in right
                // 和insert操作类似，move操作的三种情况
                // 1. moveParent在before中，这种情况出现于插入的节点插入到List中，产生新的结构
                // 2. moveParent在After中，但是在before中有对应的节点 ，这种情况出现于插入元素对原本位置元素的替换
                // 3. moveParent在before中没有对应的节点，但是在之前的操作中已经插入到right中，这种情况需要从maintenanceMap中找到对应的节点
                MoNode moveParentInRight = null;
                if(this.beforeToAfterMap.containsKey(moveParent)) {
                    logger.info("moveParent type 1");
                    MoNode moveParentType1Left = matchInstance.getNodeMap().get(moveParent);
                    if(moveParentType1Left == null) {
                        logger.error("error when Move because moveParentType1Left is null, matching error");
                        return;
                    }
                    moveParentInRight = this.leftToRightMap.get(moveParentType1Left);
                } else {
                    MoNode moveParentType2Before = this.beforeToAfterMap.getKey(moveParent);
                    if(moveParentType2Before != null) {
                        logger.info("moveParent type 2");
                        MoNode moveParentType2Left = this.matchInstance.getNodeMap().get(moveParentType2Before);
                        if(moveParentType2Left == null) {
                            logger.error("error when Move because moveParentType2Left is null, matching error");
                            return;
                        }
                        moveParentInRight = this.leftToRightMap.get(moveParentType2Left);
                    } else {
                        logger.info("insertParent type 3");
                        MoNode moveParentType3Right = maintenanceMap.getKey(moveParent);
                        if(moveParentType3Right == null) {
                            logger.error("error when Move because insertParent is not in before tree and maintenanceMap");
                            return;
                        }
                        moveParentInRight = moveParentType3Right;
                    }
                }
                assert moveParentInRight != null;


                // 尝试找到moveNode在right中的位置
                MoNode moveNodeInLeft = this.matchInstance.getNodeMap().get(moveNodeInBefore);
                if(moveNodeInLeft == null) {
                    logger.error("error when move because moveNodeInLeft is null, matching error");
                    return;
                }
                MoNode moveNodeInRight = this.leftToRightMap.get(moveNodeInLeft);
                // 有可能移除失败，由于他被其他操作移除了（例如insert的顶替）
                moveNodeInRight.removeFromParent();

                // 复制moveNodeInLeft
                DeepCopyScanner moveDeepCopyScanner = new DeepCopyScanner(moveNodeInLeft);
                MoNode moveNodeInLeftDeepCopy = moveDeepCopyScanner.getCopy();
                maintenanceMap.putAll(moveDeepCopyScanner.getCopyMap());

                // insert the move node in right
                if(moveToLocation.classification() == ChildType.CHILDLIST) {
                    MoNodeList<MoNode> children = (MoNodeList<MoNode>) moveParentInRight.getStructuralProperty(moveToLocation.role());
                    int index = moveOperation.computeIndex();
                    if(index < 0 || index > children.size()) {
                        logger.error("error when Insert because index is out of bound");
                        return;
                    }

                    children.add(index, moveNodeInLeftDeepCopy);
                } else if (moveToLocation.classification() == ChildType.CHILD) {
                    moveParentInRight.setStructuralProperty(moveToLocation.role(), moveNodeInLeftDeepCopy);
                } else {
                    logger.error("error when Insert because insertLocation is single");
                }

            } else if (operation instanceof UpdateOperation updateOperation) {
                MoNode updateNodeInBefore = updateOperation.getUpdateNode();
                String updateValue = updateOperation.getUpdateValue();

                MoNode updateNodeInLeft = this.matchInstance.getNodeMap().get(updateNodeInBefore);
                if(updateNodeInLeft == null) {
                    logger.error("error when update because updateNodeInLeft is null, matching error");
                    return;
                }
                MoNode updateNodeInRight = this.leftToRightMap.get(updateNodeInLeft);
                assert updateNodeInRight != null;

                if(setValue(updateNodeInRight, updateValue)) {
                    logger.info("update success, node type: {}", updateNodeInRight.getClass().getName());
                } else {
                    logger.error("error when update, node type {} is not supported", updateNodeInRight.getClass().getName());
                }

            } else {
                logger.error("Unknown operation type");
            }
        }
    }

    private boolean setValue(MoNode node, String value) {
        // todo: set value in different node type
        if(node instanceof MoSimpleName simpleName) {
            simpleName.setStructuralProperty("identifier", value);
            return true;
        } else if (node instanceof MoBooleanLiteral booleanLiteral) {
            booleanLiteral.setStructuralProperty("booleanValue", Boolean.parseBoolean(value));
            return true;
        } else if (node instanceof MoCharacterLiteral characterLiteral) {
            characterLiteral.setStructuralProperty("escapedValue", value);
            return true;
        } else if (node instanceof MoStringLiteral stringLiteral) {
            stringLiteral.setStructuralProperty("escapedValue", value);
            return true;
        } else if (node instanceof MoNumberLiteral numberLiteral) {
            numberLiteral.setStructuralProperty("token", value);
            return true;
        } else if (node instanceof MoModifier modifier) {
            modifier.setStructuralProperty("keyword", value);
            return true;
        }
        return false;
    }

}
