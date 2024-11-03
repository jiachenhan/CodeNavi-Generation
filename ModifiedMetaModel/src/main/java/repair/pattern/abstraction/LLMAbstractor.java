package repair.pattern.abstraction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.pattern.Pattern;
import repair.pattern.attr.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class LLMAbstractor implements Abstractor {
    private final static Logger logger = LoggerFactory.getLogger(LLMAbstractor.class);

    private final Path abstractInfoPath;
    private final List<String> consideredElements;
    private final Map<String, String> consideredAttrs;

    public LLMAbstractor(Path abstractInfoPath) {
        this.abstractInfoPath = abstractInfoPath;
        this.consideredElements = new ArrayList<>();
        this.consideredAttrs = new HashMap<>();
        parseAbstractInfo();
    }

    @Override
    public boolean shouldConsider(MoNode node) {
        // 包含了action相关的节点以及LLM考虑语义的节点
        return consideredElements.contains(String.valueOf(node.getId())) || considerNodeCandidates.contains(node);
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

        if(attribute instanceof ExprTypeAttribute exprTypeAttribute) {
            String nodeId = String.valueOf(attribute.getNode().getId());
            String attrName = exprTypeAttribute.getValue();
            if(consideredAttrs.containsKey(nodeId)) {
                String consideredAttrName = consideredAttrs.get(nodeId);
                if(!consideredAttrName.equals(attrName)) {
                    logger.warn("Node {} Attribute {} has error", nodeId, attrName);
                }
                return true;
            }
        }
        return false;
    }

    private final Set<MoNode> considerNodeCandidates = new HashSet<>();

    @Override
    public void doAbstraction(Pattern pattern) {
        Map<MoNode, Boolean> nodeToConsidered = pattern.getNodeToConsidered();
        Map<MoNode, Map<Class<? extends Attribute<?>>, Attribute<?>>> nodeToAttributes = pattern.getNodeToAttributes();

        // get action related
        List<MoNode> actionsRelatedNodes = getActionRelatedNodes(pattern);

        // expand action nodes
        actionsRelatedNodes.forEach(node -> {
            considerNodeCandidates.add(node);
            MoNode parent = node.getParent();
            // expand parent k=1
            if(parent != null) {
                considerNodeCandidates.add(parent);
            }
            // expand children k=1
            if(!node.isLeaf()) {
                considerNodeCandidates.addAll(node.getChildren());
            }

            // data flow
            if (node.context.getDataDependency() != null) {
                considerNodeCandidates.add(node.context.getDataDependency());
            }
            MoNode nodeAfter = pattern.getBeforeToAfterMap().get(node);
            if(nodeAfter != null) {
                if (nodeAfter.context.getDataDependency() != null) {
                    MoNode dataDepBefore = pattern.getBeforeToAfterMap().getKey(nodeAfter.context.getDataDependency());
                    considerNodeCandidates.add(dataDepBefore);
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

    }

    private void parseAbstractInfo() {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // 读取 JSON 文件并反序列化为 JsonNode
            JsonNode rootNode = objectMapper.readTree(this.abstractInfoPath.toFile());

            // 解析 considered_elements
            JsonNode consideredElementsNode = rootNode.get("considered_elements");
            for (JsonNode element : consideredElementsNode) {
                consideredElements.add(element.asText());
            }

            // 解析 considered_attrs
            JsonNode consideredAttrsNode = rootNode.get("considered_attrs");
            Iterator<Map.Entry<String, JsonNode>> fields = consideredAttrsNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                consideredAttrs.put(field.getKey(), field.getValue().asText());
            }

        } catch (IOException e) {
            logger.error("Failed to read abstract info file");
        }
    }

}
