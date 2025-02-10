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

    public LLMAbstractor(Path abstractInfoPath) {
        this.abstractInfoPath = abstractInfoPath;
        this.LLMConsideredElements = new ArrayList<>();
        this.LLMConsideredAttrs = new HashMap<>();

        parseAbstractInfo();
    }

    @Override
    public boolean shouldConsider(MoNode node) {
        // 包含了action相关的节点以及LLM考虑语义的节点
//        return LLMConsideredElements.contains(String.valueOf(node.getId())) || actionRelatedConsiderNodes.contains(node);
        return LLMConsideredElements.contains(String.valueOf(node.getId()));
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
            MoNode parent = node.getParent();
            // expand parent k=1
            if(parent != null) {
                actionRelatedConsiderNodes.add(parent);
            }
            // expand children k=1
            if(!node.isLeaf()) {
                actionRelatedConsiderNodes.addAll(node.getChildren());
            }

            // data flow
            if (node.context.getDataDependency() != null) {
                actionRelatedConsiderNodes.add(node.context.getDataDependency());
            }
            MoNode nodeAfter = pattern.getBeforeToAfterMap().get(node);
            if(nodeAfter != null) {
                if (nodeAfter.context.getDataDependency() != null) {
                    MoNode dataDepBefore = pattern.getBeforeToAfterMap().getKey(nodeAfter.context.getDataDependency());
                    actionRelatedConsiderNodes.add(dataDepBefore);
                }
            }

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

        // insert or move nodes abstraction
        pattern.getNotLogicManager().ifPresent(notLogicManager -> {
            notLogicManager.getInsertNodes().forEach(insertNode -> {
            });
        });

    }

    private void parseAbstractInfo() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // 读取 JSON 文件并反序列化为 JsonNode
            JsonNode rootNode = objectMapper.readTree(this.abstractInfoPath.toFile());

            // 解析 considered_elements
            JsonNode consideredElementsNode = rootNode.get("considered_elements");
            for (JsonNode element : consideredElementsNode) {
                LLMConsideredElements.add(element.asText());
            }

            // 解析 considered_attrs
            JsonNode consideredAttrsNode = rootNode.get("considered_attrs");
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
        } catch (IOException e) {
            logger.error("Failed to read abstract info file");
        }
    }

}
