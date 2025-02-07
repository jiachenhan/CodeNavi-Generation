package repair.dsl.kirin;

import repair.ast.MoNode;
import repair.dsl.kirin.map.DSLNodeMapping;
import repair.dsl.kirin.map.DSLRoleMapping;
import repair.dsl.kirin.map.code.KeyWord;
import repair.dsl.kirin.map.code.KeyWordFactory;
import repair.dsl.kirin.map.code.node.DSLNode;
import repair.dsl.kirin.map.code.role.DSLRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NodePath {
    private final MoNode rootNode;
    private final MoNode considerNode;
    private List<NodeDSLBundle> nodePath;
    private List<? extends DSLRole> rolePath;

    public NodePath(MoNode funcDecl, MoNode considerNode) {
        this.rootNode = funcDecl;
        this.considerNode = considerNode;
    }

    public MoNode getRootNode() {
        return rootNode;
    }

    public MoNode getConsiderNode() {
        return considerNode;
    }

    public List<NodeDSLBundle> getNodePath() {
        return nodePath;
    }

    public List<? extends DSLRole> getRolePath() {
        return rolePath;
    }

    public static NodePath computeNodePath(MoNode funcDecl, MoNode subConsiderNode, List<MoNode> consideredNodes) {
        NodePath result = new NodePath(funcDecl, subConsiderNode);

        // 先从叶子节点向上走，添加节点路径直到funcDecl
        List<NodeDSLBundle> nodePath = new ArrayList<>();
        DSLNode dslNode = DSLNodeMapping.convertMoNode2DSLNode(subConsiderNode);
        nodePath.add(new NodeDSLBundle(true, subConsiderNode, dslNode));

        MoNode parent = subConsiderNode.getParent();
        // 保证一条路径上至少有一个末端节点可以映射成DSL代码
        boolean isSubQueryConsider = false;
        while (parent != null && !parent.isSame(funcDecl)) {
            boolean considerParent = consideredNodes.contains(parent);
            DSLNode parentDSLNode = DSLNodeMapping.convertMoNode2DSLNode(parent);
            // 将有关键词映射的节点设置为需要考虑，防止最末端属性（name等）没有实体节点，生成DSL不正确
            if (! isSubQueryConsider && parentDSLNode instanceof KeyWord) {
                considerParent = true;
                isSubQueryConsider = true;
            }
            nodePath.add(new NodeDSLBundle(considerParent, parent, parentDSLNode));
            parent = parent.getParent();
        }

        nodePath.add(new NodeDSLBundle(false, funcDecl, DSLNodeMapping.convertMoNode2DSLNode(funcDecl)));
        Collections.reverse(nodePath);
        result.nodePath = nodePath;

        // set rolePath
        result.rolePath = result.nodePath.stream()
                .skip(1)
                .map(NodeDSLBundle::getOriginalNode)
                .map(node -> DSLRoleMapping.convertRelationRole(node.getParent(), node))
                .map(KeyWordFactory::createRoleInstance)
                .toList();

        return result;
    }
}
