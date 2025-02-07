package repair.dsl.kirin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.code.statement.MoBlock;
import repair.ast.code.type.MoPrimitiveType;
import repair.ast.code.type.MoSimpleType;
import repair.dsl.kirin.condition.BinaryCondition;
import repair.dsl.kirin.condition.NotCondition;
import repair.dsl.kirin.expr.*;
import repair.dsl.kirin.map.DSLNodeMapping;
import repair.dsl.kirin.map.DSLRoleMapping;
import repair.dsl.kirin.map.code.KeyWord;
import repair.dsl.kirin.map.code.KeyWordFactory;
import repair.dsl.kirin.map.code.Nameable;
import repair.dsl.kirin.map.code.node.DSLNode;
import repair.dsl.kirin.map.code.role.DSLRole;
import repair.dsl.kirin.map.code.role.RoleAction;
import repair.dsl.kirin.query.AliasQuery;
import repair.dsl.kirin.query.NormalQuery;
import repair.dsl.kirin.query.Query;
import repair.dsl.kirin.query.TemplateQuery;
import repair.pattern.InsertNode;
import repair.pattern.MoveNode;
import repair.pattern.NotLogicManager;
import repair.pattern.Pattern;
import repair.pattern.attr.Attribute;

import java.util.*;

public class QueryGenerator {
    private static final Logger logger = LoggerFactory.getLogger(QueryGenerator.class);
    private static QueryGenerator queryGenerator;

    private QueryGenerator() {
    }

    public static synchronized QueryGenerator getInstance() {
        if (queryGenerator == null) {
            queryGenerator = new QueryGenerator();
        }
        return queryGenerator;
    }


    public Query generate(Pattern graphPattern) {
        logger.info("Generating query");
        MoNode patternBefore0 = graphPattern.getPatternBefore0();
        List<MoNode> consideredNodes = graphPattern.getConsideredNodes();
        Map<MoNode, Map<Class<? extends Attribute<?>>, Attribute<?>>> nodeToAttributes = graphPattern.getNodeToAttributes();

        List<NodePath> nodePaths = collectConsideredNodePaths(patternBefore0, consideredNodes);

        TemplateQuery templateQuery = new TemplateQuery(patternBefore0);
        Map<MoNode, Query> queryMap = new HashMap<>();
        queryMap.put(patternBefore0, templateQuery);
        setConditionsTopdown(templateQuery, nodePaths, queryMap);

        Optional<NotLogicManager> notLogicManagerOpt = graphPattern.getNotLogicManager();
        if (notLogicManagerOpt.isEmpty()) {
            return templateQuery;
        }
        NotLogicManager notLogicManager = notLogicManagerOpt.get();
        for (InsertNode insertNode : notLogicManager.getInsertNodes()) {
            generateInsertNotConditions(insertNode, queryMap);
        }

        for (MoveNode moveNode : notLogicManager.getMoveNodes()) {
            generateMoveNotConditions(moveNode, queryMap);
        }

        return templateQuery;
    }

    private List<NodePath> collectConsideredNodePaths(MoNode patternBefore, List<MoNode> consideredNodes) {
        return consideredNodes.stream()
                .filter(node -> node != patternBefore)
                .map(node -> NodePath.computeNodePath(patternBefore, node, consideredNodes))
                .toList();
    }

    private void generateInsertNotConditions(InsertNode insertNode, Map<MoNode, Query> queryMap) {
        MoNode insertParent = insertNode.insertParent();
        MoNode insertedNode = insertNode.insertNode();

        DSLNode dslNode = DSLNodeMapping.convertMoNode2DSLNode(insertedNode);
        if (! (dslNode instanceof KeyWord)) {
            return;
        }

        List<MoNode> consideredNodes = insertNode.insertConsideredNode().keySet().stream().toList();
        List<NodePath> nodePaths = consideredNodes.stream()
                .filter(node -> node != insertedNode)
                .map(node -> NodePath.computeNodePath(insertedNode, node, consideredNodes))
                .toList();

        List<DSLRole> roleList = new ArrayList<>();
        if (insertParent instanceof MoBlock) {
            DSLRole role = KeyWordFactory.createRoleInstance(DSLRoleMapping.convertRelationRole(insertParent.getParent(), insertParent));
            roleList.add(role);
            insertParent = insertParent.getParent();
        }

        if (queryMap.containsKey(insertParent)) {
            Query query = queryMap.get(insertParent);

            Query notQuery = new NormalQuery(insertedNode, dslNode);
            setConditionsTopdown(notQuery, nodePaths, queryMap);

            RoleListExpr aliasExpr = new RoleListExpr(query.getAlias(), roleList);
            BinaryCondition containCondition = new BinaryCondition(BinaryCondition.Predicate.CONTAIN, aliasExpr, notQuery);
            NotCondition notCondition = new NotCondition(containCondition);
            query.addCondition(notCondition);
        }
    }

    private void generateMoveNotConditions(MoveNode moveNode, Map<MoNode, Query> queryMap) {
        MoNode movedNode = moveNode.moveNode();
        MoNode moveParent = moveNode.moveParent();

        DSLNode dslNode = DSLNodeMapping.convertMoNode2DSLNode(moveParent);
        if (! (dslNode instanceof KeyWord)) {
            return;
        }

        List<MoNode> consideredNodes = moveNode.moveParentConsideredNode().keySet().stream().toList();
        List<NodePath> nodePaths = consideredNodes.stream()
                .filter(node -> node != moveParent)
                .map(node -> NodePath.computeNodePath(moveParent, node, consideredNodes))
                .toList();

        if (queryMap.containsKey(movedNode)) {
            Query query = queryMap.get(movedNode);
            Lhs aliasExpr = new AliasExpr(query.getAlias());

            NormalQuery notQuery = new NormalQuery(moveParent, dslNode);
            setConditionsTopdown(notQuery, nodePaths, queryMap);

            if (notQuery.getCondition().isEmpty()) {
                // 特殊处理5d4d2ae10d54444ab74a6faec78acaa2 多notin相同节点，如果不生成具体条件，不能使用别名
                BinaryCondition containCondition = new BinaryCondition(BinaryCondition.Predicate.NOT_IN, aliasExpr, new DSLNodeExpr(notQuery.getDslNode().prettyPrint()));
                query.addCondition(containCondition);
            } else {
                BinaryCondition containCondition = new BinaryCondition(BinaryCondition.Predicate.NOT_IN, aliasExpr, notQuery);
                query.addCondition(containCondition);
            }

        }
    }

    private void setConditionsTopdown(Query query, List<NodePath> nodePaths, Map<MoNode, Query> queryMap) {
        for (NodePath nodePath : nodePaths) {
            if (nodePath.getRolePath().stream().anyMatch(role -> role.getRoleAction() == RoleAction.Interrupt)) {
                continue;
            }

            Query currentQuery = query;
            List<DSLRole> roleList = new ArrayList<>();
            RoleAction state = RoleAction.Child;
            NodeDSLBundle currentNodeBundle = null;
            for (int i = 0; i < nodePath.getNodePath().size(); i++) {
                currentNodeBundle = nodePath.getNodePath().get(i);
                if (state == RoleAction.Collection) {
                    if (! currentNodeBundle.isConsider()) {
                        continue;
                    }

                    Query aliasQuery;
                    if (queryMap.containsKey(currentNodeBundle.getOriginalNode())) {
                        aliasQuery = queryMap.get(currentNodeBundle.getOriginalNode());
                    } else {
                        aliasQuery = new AliasQuery(currentNodeBundle.getOriginalNode());
                        queryMap.put(currentNodeBundle.getOriginalNode(), aliasQuery);

                        if (currentNodeBundle.getDslNode() instanceof KeyWord) {
                            RoleListExpr aliasExpr = new RoleListExpr(aliasQuery.getAlias(), List.of());
                            DSLNodeExpr typeExpr = new DSLNodeExpr(currentNodeBundle.getDslNode().prettyPrint());
                            aliasQuery.addCondition(new BinaryCondition(BinaryCondition.Predicate.IS, aliasExpr, typeExpr));
                        }

                        RoleListExpr roleListExpr = new RoleListExpr(currentQuery.getAlias(), new ArrayList<>(roleList));
                        currentQuery.addCondition(new BinaryCondition(BinaryCondition.Predicate.CONTAIN, roleListExpr, aliasQuery));
                    }
                    currentQuery = aliasQuery;
                    roleList.clear();
                } else if (state == RoleAction.Body) {
                    if (! currentNodeBundle.isConsider()) {
                        continue;
                    }

                    if (! (currentNodeBundle.getDslNode() instanceof KeyWord)) {
                        continue;
                    }

                    Query normalQuery;
                    if (queryMap.containsKey(currentNodeBundle.getOriginalNode())) {
                        normalQuery = queryMap.get(currentNodeBundle.getOriginalNode());
                    } else {
                        normalQuery = new NormalQuery(currentNodeBundle.getOriginalNode(), currentNodeBundle.getDslNode());
                        queryMap.put(currentNodeBundle.getOriginalNode(), normalQuery);

                        RoleListExpr roleListExpr = new RoleListExpr(currentQuery.getAlias(), new ArrayList<>(roleList));
                        currentQuery.addCondition(new BinaryCondition(BinaryCondition.Predicate.CONTAIN, roleListExpr, normalQuery));
                    }

                    currentQuery = normalQuery;
                    roleList.clear();
                }

                if (i < nodePath.getNodePath().size() - 1) {
                    DSLRole dslRole = nodePath.getRolePath().get(i);
                    if (dslRole.getRoleAction() != RoleAction.Skip) {
                        roleList.add(dslRole);
                    }
                    state = dslRole.getRoleAction();
                } else {
                    state = RoleAction.Child;
                }
            }

            // name attr
            // 被考虑节点路径被中间截断
            if ((state != RoleAction.Collection && state != RoleAction.Body)
                    && currentNodeBundle != null && currentNodeBundle.getDslNode() instanceof Nameable nameable) {
                RoleListExpr roleListExpr = new RoleListExpr(currentQuery.getAlias(), new ArrayList<>(roleList));
                MoNode moNode = currentNodeBundle.getOriginalNode();
                Nameable.NameAttr nameAttr = nameable.getNameAttr(moNode);

                BinaryCondition binaryCondition;
                boolean isSimpleType = currentNodeBundle.getOriginalNode().getParent() instanceof MoSimpleType;
                if (isSimpleType) {
                    Rhs rhs = new StringExpr(".*\\." + nameAttr.valueName());
                    binaryCondition = new BinaryCondition(BinaryCondition.Predicate.MATCH, new NameAttrExpr(roleListExpr, nameAttr.keyName()), rhs);
                } else if (currentNodeBundle.getOriginalNode() instanceof MoPrimitiveType) {
                    Rhs rhs = new StringExpr(nameAttr.valueName());
                    binaryCondition = new BinaryCondition(BinaryCondition.Predicate.MATCH, new NameAttrExpr(roleListExpr, nameAttr.keyName()), rhs);
                } else {
                    if (nameAttr.hasQuotationMark()) {
                        Rhs rhs = new StringExpr(nameAttr.valueName());
                        binaryCondition = new BinaryCondition(BinaryCondition.Predicate.EQ, new NameAttrExpr(roleListExpr, nameAttr.keyName()), rhs);
                    } else {
                        Rhs rhs = new DSLNodeExpr(nameAttr.valueName());
                        binaryCondition = new BinaryCondition(BinaryCondition.Predicate.EQ, new NameAttrExpr(roleListExpr, nameAttr.keyName()), rhs);
                    }
                }
                currentQuery.addCondition(binaryCondition);
            }
        }
    }
}
