package repair.pattern.abstraction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.code.expression.MoName;
import repair.pattern.Pattern;
import repair.pattern.attr.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class LLMAbstractor implements Abstractor {
    private final static Logger logger = LoggerFactory.getLogger(LLMAbstractor.class);

    private final Path abstractInfoPath;
    private final List<String> LLMConsideredElements;
    private final Map<String, List<String>> LLMConsideredAttrs;

    private final Map<String, String> LLMGuessRegexMap;

    private final Map<String, List<String>> LLMConsideredInsertElement;
    private final Map<String, List<String>> LLMConsideredMoveElement;

    public LLMAbstractor(Path abstractInfoPath) {
        this.abstractInfoPath = abstractInfoPath;
        this.LLMConsideredElements = new ArrayList<>();
        this.LLMConsideredAttrs = new HashMap<>();

        this.LLMGuessRegexMap = new HashMap<>();
        this.LLMConsideredInsertElement = new HashMap<>();
        this.LLMConsideredMoveElement = new HashMap<>();
        parseAbstractInfo();
    }

    @Override
    public boolean shouldConsider(MoNode node) {
        // 包含了action相关的节点以及LLM考虑语义的节点
        return LLMConsideredElements.contains(String.valueOf(node.getId())) || actionRelatedConsiderNodes.contains(node);
//        return LLMConsideredElements.contains(String.valueOf(node.getId()));
    }

    @Override
    public boolean shouldConsider(Attribute<?> attribute) {
        if(attribute instanceof LocationSubTypeAttribute) {
            return true;
        }
        if(attribute instanceof MoTypeAttribute) {
            return true;
        }
        if(attribute instanceof TokenAttribute) {
            return true;
        }

        if(attribute instanceof NameAttribute) {
            MoNode node = attribute.getNode();
            return node instanceof MoName && actionRelatedConsiderNodes.contains(node);
        }

        if(attribute instanceof ExprTypeAttribute exprTypeAttribute) {
            String nodeId = String.valueOf(attribute.getNode().getId());
            String attrName = exprTypeAttribute.getValue();
            List<String> consideredExprTypeNodes = LLMConsideredAttrs.get("exprType");
            return consideredExprTypeNodes.contains(nodeId);
        }
        return false;
    }

    private final Set<MoNode> actionRelatedConsiderNodes = new HashSet<>();

    @Override
    public void doAbstraction(Pattern pattern) {
        Map<MoNode, Boolean> nodeToConsidered = pattern.getNodeToConsidered();
        Map<MoNode, Map<Class<? extends Attribute<?>>, Attribute<?>>> nodeToAttributes = pattern.getNodeToAttributes();

        // get action related
        List<MoNode> actionsRelatedNodes = getActionRelatedNodes(pattern);

        // expand action nodes
        actionsRelatedNodes.forEach(node -> {
            actionRelatedConsiderNodes.add(node);
//            MoNode parent = node.getParent();
//            // expand parent k=1
//            if(parent != null) {
//                actionRelatedConsiderNodes.add(parent);
//            }
//            // expand children k=1
//            if(!node.isLeaf()) {
//                actionRelatedConsiderNodes.addAll(node.getChildren());
//            }
//
//            // data flow
//            if (node.context.getDataDependency() != null) {
//                actionRelatedConsiderNodes.add(node.context.getDataDependency());
//            }
//            MoNode nodeAfter = pattern.getBeforeToAfterMap().get(node);
//            if(nodeAfter != null) {
//                if (nodeAfter.context.getDataDependency() != null) {
//                    MoNode dataDepBefore = pattern.getBeforeToAfterMap().getKey(nodeAfter.context.getDataDependency());
//                    actionRelatedConsiderNodes.add(dataDepBefore);
//                }
//            }
        });

        nodeToConsidered.forEach((node, value) -> {
            boolean shouldConsider = shouldConsider(node);
            nodeToConsidered.put(node, shouldConsider);
            if(shouldConsider) {
                Map<Class<? extends Attribute<?>>, Attribute<?>> attributes = nodeToAttributes.get(node);
                attributes.forEach((attrClass, attr) -> {
                    attr.setConsidered(shouldConsider(attr));
                });
            }
        });

        if (! LLMGuessRegexMap.isEmpty()) {
            pattern.setNodeIdToRegex(LLMGuessRegexMap);
        }

        // insert or move nodes abstraction
        pattern.getNotLogicManager().ifPresent(notLogicManager -> {
            notLogicManager.getInsertNodes().forEach(insertNode -> {
                MoNode insertedNode = insertNode.insertNode();
                Map<MoNode, Boolean> insertNodeToConsidered = insertNode.insertConsideredNode();
                int insertId = insertedNode.getId();
                if (LLMConsideredInsertElement.containsKey(String.valueOf(insertId))) {
                    List<String> consideredSubInsertNodes = LLMConsideredInsertElement.get(String.valueOf(insertId));
                    insertNodeToConsidered.forEach((node, value) -> {
                        if (consideredSubInsertNodes.contains(String.valueOf(node.getId()))) {
                            insertNodeToConsidered.put(node, true);
                        } else {
                            insertNodeToConsidered.put(node, false);
                        }
                    });
                }
            });

            notLogicManager.getMoveNodes().forEach(moveNode -> {
                MoNode movedNode = moveNode.moveNode();
                Map<MoNode, Boolean> moveNodeToConsidered = moveNode.moveParentConsideredNode();
                int moveId = movedNode.getId();
                if (LLMConsideredMoveElement.containsKey(String.valueOf(moveId))) {
                    List<String> consideredSubMoveNodes = LLMConsideredMoveElement.get(String.valueOf(moveId));
                    moveNodeToConsidered.forEach((node, value) -> {
                        if (consideredSubMoveNodes.contains(String.valueOf(node.getId()))) {
                            moveNodeToConsidered.put(node, true);
                        } else {
                            moveNodeToConsidered.put(node, false);
                        }
                    });
                }
            });
        });

    }

    private void parseAbstractInfo() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // 读取 JSON 文件并反序列化为 JsonNode
            JsonNode rootNode = objectMapper.readTree(this.abstractInfoPath.toFile());
            // 解析 considered_elements
            extractElements(rootNode.get("considered_elements"));
            // 解析 considered_attrs
            extractAttrs(rootNode.get("considered_attrs"));

            // 解析 regex
            extractGuessRegex(rootNode.get("regex"));
            // 解析 insert_considered_elements
            extractInsertElements(rootNode.get("insert_elements"));
            // 解析 move_considered_elements
            extractMoveElements(rootNode.get("move_elements"));
        } catch (IOException e) {
            logger.error("Failed to read abstract info file");
        }
    }

    private void extractAttrs(JsonNode consideredAttrsNode) {
        Iterator<Map.Entry<String, JsonNode>> attrs = consideredAttrsNode.fields();
        while (attrs.hasNext()) {
            Map.Entry<String, JsonNode> attr = attrs.next();
            LLMConsideredAttrs.put(attr.getKey(), new ArrayList<>());
            Iterator<JsonNode> nodes = attr.getValue().elements();
            while (nodes.hasNext()) {
                String id = nodes.next().asText();
                LLMConsideredAttrs.get(attr.getKey()).add(id);
            }
        }
    }

    private void extractElements(JsonNode consideredElementsNode) {
        // 解析 considered_elements
        for (JsonNode element : consideredElementsNode) {
            LLMConsideredElements.add(element.asText());
        }
    }

    private void extractGuessRegex(JsonNode regexNode) {
        Iterator<Map.Entry<String, JsonNode>> regexNames = regexNode.fields();
        while (regexNames.hasNext()) {
            Map.Entry<String, JsonNode> regexName = regexNames.next();
            LLMGuessRegexMap.put(regexName.getKey(), regexName.getValue().asText());
        }
    }

    private void extractInsertElements(JsonNode consideredInsertNode) {
        Iterator<Map.Entry<String, JsonNode>> insertNodes = consideredInsertNode.fields();
        while (insertNodes.hasNext()) {
            Map.Entry<String, JsonNode> insert = insertNodes.next();
            LLMConsideredInsertElement.put(insert.getKey(), new ArrayList<>());
            Iterator<JsonNode> nodes = insert.getValue().elements();
            while (nodes.hasNext()) {
                String id = nodes.next().asText();
                LLMConsideredInsertElement.get(insert.getKey()).add(id);
            }
        }
    }

    private void extractMoveElements(JsonNode consideredMoveNode) {
        Iterator<Map.Entry<String, JsonNode>> moveNodes = consideredMoveNode.fields();
        while (moveNodes.hasNext()) {
            Map.Entry<String, JsonNode> move = moveNodes.next();
            LLMConsideredMoveElement.put(move.getKey(), new ArrayList<>());
            Iterator<JsonNode> nodes = move.getValue().elements();
            while (nodes.hasNext()) {
                String id = nodes.next().asText();
                LLMConsideredMoveElement.get(move.getKey()).add(id);
            }
        }
    }

}
