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
import repair.ast.code.expression.MoQualifiedName;
import repair.ast.code.expression.MoSimpleName;
import repair.ast.code.expression.literal.MoBooleanLiteral;
import repair.ast.code.expression.literal.MoCharacterLiteral;
import repair.ast.code.expression.literal.MoNumberLiteral;
import repair.ast.code.expression.literal.MoStringLiteral;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.visitor.DeepCopyScanner;
import repair.ast.visitor.FlattenScanner;
import repair.modify.builder.GumtreeMetaConstant;
import repair.modify.diff.operations.*;
import repair.pattern.Pattern;

import java.util.HashSet;
import java.util.Set;

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

    private final Set<MoNode> placeholderNodesToBeRemoved = new HashSet<>();


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

            } else if (operation instanceof TreeDeleteOperation treeDeleteOperation) {
                MoNode deleteNodeInBefore = treeDeleteOperation.getDeleteNodeInBefore();
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

                // insert 的时候，对应节点还没有删去，insert里还是有原来的节点, 需要先对maintenanceMap中的节点进行匹配
                // TODO: 如果是将pattern应用到right上，那么新插入的节点也先暂时不能改名，而是在全部操作完成后，再分析上下文进行改名
                MoNode insertParentInRight = null;
                if(maintenanceMap.containsKey(insertParent)) {
                    // 优先级最高
                    logger.info("insertParent type 3");
                    insertParentInRight = maintenanceMap.get(insertParent);
                } else if(this.beforeToAfterMap.containsKey(insertParent)) {
                    logger.info("insertParent type 1");
                    MoNode insertParentType1Left = matchInstance.getNodeMap().get(insertParent);
                    if(insertParentType1Left == null) {
                        logger.error("error when Insert because insertParentType1Left is null, matching error");
                        return;
                    }
                    insertParentInRight = this.leftToRightMap.get(insertParentType1Left);
                } else if(this.beforeToAfterMap.containsValue(insertParent)){
                    MoNode insertParentType2Before = this.beforeToAfterMap.getKey(insertParent);
                    logger.info("insertParent type 2");
                    MoNode insertParentType2Left = this.matchInstance.getNodeMap().get(insertParentType2Before);
                    if(insertParentType2Left == null) {
                        logger.error("error when Insert because insertParentType2Left is null, matching error");
                        return;
                    }
                    insertParentInRight = this.leftToRightMap.get(insertParentType2Left);
                    logger.error("error when Insert because insertParent is not in before tree and maintenanceMap");
                } else {
                    logger.error("error when Insert because insertParent is not in before tree and maintenanceMap");
                }
                assert insertParentInRight != null;
                assert inRightTree(insertParentInRight);

                // generate the insertee node in right
                MoNode insertNodeTemplate = insertOperation.getAddNode();
                MoNode insertNodeInRight = null;
                if(insertNodeTemplate instanceof MoQualifiedName moQualifiedName) {
                    DeepCopyScanner copyScanner = new DeepCopyScanner(moQualifiedName);
                    insertNodeInRight = copyScanner.getCopy();
                } else {
                    insertNodeInRight = insertNodeTemplate.shallowClone();
                }

                maintenanceMap.put(insertNodeTemplate, insertNodeInRight);


                // insert the insertee node in right
                if(insertLocation.classification() == ChildType.CHILDLIST) {
                    MoNodeList<MoNode> children = (MoNodeList<MoNode>) insertParentInRight.getStructuralProperty(insertLocation.role());
                    int index = insertOperation.computeIndex();
                    makeSureInsertIndex(index, children, insertNodeInRight);
                    children.add(index, insertNodeInRight);
                    insertNodeInRight.setParent(insertParentInRight, insertLocation);
                } else if (insertLocation.classification() == ChildType.CHILD) {
                    insertParentInRight.setStructuralProperty(insertLocation.role(), insertNodeInRight);
                    insertNodeInRight.setParent(insertParentInRight, insertLocation);
                } else {
                    logger.error("error when Insert because insertLocation is single");
                }
            } else if (operation instanceof TreeInsertOperation treeInsertOperation) {
                MoNode insertParent = treeInsertOperation.getParent();
                Description<? extends MoNode, ?> insertLocation = treeInsertOperation.getLocation();

                // find the insertParent in right
                // 对于insert操作有三种情况
                // 1. insertParent在before中，这种情况出现于插入的节点插入到List中，产生新的结构
                // 2. insertParent在After中，但是在before中有对应的节点 ，这种情况出现于插入元素对原本位置元素的替换
                // 3. insertParent在before中没有对应的节点，但是在之前的操作中已经插入到right中，这种情况需要从maintenanceMap中找到对应的节点
                MoNode insertParentInRight = null;
                if(maintenanceMap.containsKey(insertParent)) {
                    // 优先级最高
                    logger.info("insertParent type 3");
                    insertParentInRight = maintenanceMap.get(insertParent);
                }
                else if(this.beforeToAfterMap.containsKey(insertParent)) {
                    logger.info("insertParent type 1");
                    MoNode insertParentType1Left = matchInstance.getNodeMap().get(insertParent);
                    if(insertParentType1Left == null) {
                        logger.error("error when Insert because insertParentType1Left is null, matching error");
                        return;
                    }
                    insertParentInRight = this.leftToRightMap.get(insertParentType1Left);
                } else if(this.beforeToAfterMap.containsValue(insertParent)) {
                    MoNode insertParentType2Before = this.beforeToAfterMap.getKey(insertParent);
                    logger.info("insertParent type 2");
                    MoNode insertParentType2Left = this.matchInstance.getNodeMap().get(insertParentType2Before);
                    if(insertParentType2Left == null) {
                        logger.error("error when Insert because insertParentType2Left is null, matching error");
                        return;
                    }
                    insertParentInRight = this.leftToRightMap.get(insertParentType2Left);
                } else {
                    logger.error("error when Insert because insertParent is not in before tree and maintenanceMap");
                }
                assert insertParentInRight != null;
                assert inRightTree(insertParentInRight);


                // generate the insertee node in right
                MoNode insertNodeTemplate = treeInsertOperation.getAddNode();
                DeepCopyScanner deepCopyScanner = new DeepCopyScanner(insertNodeTemplate);
                MoNode insertNodeInRight = deepCopyScanner.getCopy();
                maintenanceMap.putAll(deepCopyScanner.getCopyMap());

                // insert the insertee node in right
                if(insertLocation.classification() == ChildType.CHILDLIST) {
                    MoNodeList<MoNode> children = (MoNodeList<MoNode>) insertParentInRight.getStructuralProperty(insertLocation.role());
                    int index = treeInsertOperation.computeIndex();
                    makeSureInsertIndex(index, children, insertNodeInRight);
                    children.add(index, insertNodeInRight);
                    insertNodeInRight.setParent(insertParentInRight, insertLocation);
                } else if (insertLocation.classification() == ChildType.CHILD) {
                    insertParentInRight.setStructuralProperty(insertLocation.role(), insertNodeInRight);
                    insertNodeInRight.setParent(insertParentInRight, insertLocation);
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
                if(maintenanceMap.containsKey(moveParent)) {
                    // 优先级最高
                    logger.info("moveParent type 3");
                    moveParentInRight = maintenanceMap.get(moveParent);
                } else if(this.beforeToAfterMap.containsKey(moveParent)) {
                    logger.info("moveParent type 1");
                    MoNode moveParentType1Left = matchInstance.getNodeMap().get(moveParent);
                    if(moveParentType1Left == null) {
                        logger.error("error when Move because moveParentType1Left is null, matching error");
                        return;
                    }
                    moveParentInRight = this.leftToRightMap.get(moveParentType1Left);
                } else if (this.beforeToAfterMap.containsValue(moveParent)){
                    MoNode moveParentType2Before = this.beforeToAfterMap.getKey(moveParent);
                    logger.info("moveParent type 2");
                    MoNode moveParentType2Left = this.matchInstance.getNodeMap().get(moveParentType2Before);
                    if(moveParentType2Left == null) {
                        logger.error("error when Move because moveParentType2Left is null, matching error");
                        return;
                    }
                    moveParentInRight = this.leftToRightMap.get(moveParentType2Left);
                } else {
                    logger.error("error when Move because moveParent is not in before tree and maintenanceMap");
                }
                assert moveParentInRight != null;
                assert inRightTree(moveParentInRight);


                // 尝试找到moveNode在right中的位置
                MoNode moveNodeInLeft = this.matchInstance.getNodeMap().get(moveNodeInBefore);
                if(moveNodeInLeft == null) {
                    logger.error("error when move because moveNodeInLeft is null, matching error");
                    return;
                }
                MoNode moveNodeInRight = this.leftToRightMap.get(moveNodeInLeft);
                if(inRightTree(moveNodeInRight)) {
                    // 如果还在right树上，那么把他从right树上移除，不然直接移动就可以
                    moveNodeInRight.removeFromParent();
                }

                // insert the move node in right
                if(moveToLocation.classification() == ChildType.CHILDLIST) {
                    MoNodeList<MoNode> children = (MoNodeList<MoNode>) moveParentInRight.getStructuralProperty(moveToLocation.role());
                    int index = moveOperation.computeIndex();
                    makeSureInsertIndex(index, children, moveNodeInRight);
                    children.add(index, moveNodeInRight);
                    moveNodeInRight.setParent(moveParentInRight, moveToLocation);
                } else if (moveToLocation.classification() == ChildType.CHILD) {
                    moveParentInRight.setStructuralProperty(moveToLocation.role(), moveNodeInRight);
                    moveNodeInRight.setParent(moveParentInRight, moveToLocation);
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
                assert inRightTree(updateNodeInRight);

                if(setValue(updateNodeInRight, updateValue)) {
                    logger.info("update success, node type: {}", updateNodeInRight.getClass().getName());
                } else {
                    logger.error("error when update, node type {} is not supported", updateNodeInRight.getClass().getName());
                }

            } else {
                logger.error("Unknown operation type");
            }
        }

        // remove all placeholder nodes
        placeholderNodesToBeRemoved.forEach(MoNode::removeFromParent);
    }

    /**
     * make sure the index is in the bound of children list, by adding placeholder nodes
     * @param index the index to be inserted
     * @param children the children list
     * @param insertNodeInList the node to be inserted
     */
    private void makeSureInsertIndex(int index, MoNodeList<MoNode> children, MoNode insertNodeInList) {
        if(index < 0 || index > children.size()) {
            for (int i = 0; i < index - children.size(); i++) {
                MoNode placeholder = insertNodeInList.shallowClone();
                placeholderNodesToBeRemoved.add(placeholder);
                children.add(placeholder);
            }
        }
    }

    private boolean setValue(MoNode node, String value) {
        // todo: set value in different node type
        if(node instanceof MoSimpleName simpleName) {
            simpleName.setStructuralProperty("identifier", value);
            return true;
        } else if (node instanceof MoQualifiedName qualifiedName) {
            qualifiedName.setIdentifier(value);
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

    private void removeAllNodeFromMaintenanceMap(MoNode node) {
        FlattenScanner scanner = new FlattenScanner();
        scanner.flatten(node).forEach(maintenanceMap::removeValue);
    }

    private boolean inRightTree(MoNode node) {
        FlattenScanner scanner = new FlattenScanner();
        return scanner.flatten(right).contains(node);
    }

    public String whichTree(MoNode node) {
        FlattenScanner scanner = new FlattenScanner();
        if (scanner.flatten(pattern.getPatternBefore0()).contains(node)) {
            return "patternBefore0";
        } else if (scanner.flatten(pattern.getPatternAfter0()).contains(node)) {
            return "getPatternAfter0";
        } else if (scanner.flatten(left).contains(node)) {
            return "left";
        } else if (scanner.flatten(right).contains(node)) {
            return "right";
        } else {
            return "not found";
        }
    }

}
