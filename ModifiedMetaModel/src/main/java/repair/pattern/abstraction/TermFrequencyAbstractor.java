package repair.pattern.abstraction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.FileUtils;
import repair.ast.MoNode;
import repair.ast.code.expression.MoExpression;
import repair.ast.code.expression.MoMethodInvocation;
import repair.ast.code.expression.MoName;
import repair.ast.code.type.MoType;
import repair.pattern.Pattern;
import repair.pattern.attr.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class TermFrequencyAbstractor implements Abstractor {
    private final static Logger logger = LoggerFactory.getLogger(TermFrequencyAbstractor.class);
    private static final AttributeConfig genPatAttrConfig;
    private final static Map<String, Integer> nameMap;
    private final static Map<String, Integer> apiMap;
    private final static Map<String, Integer> typeMap;
    private final static int TOTAL_FILE_NUM = 1217392;
    private final static double threshold = 0.005;

    static {
        genPatAttrConfig = new AttributeConfig();
        genPatAttrConfig.addAttribute("LocationSubTypeAttribute", LocationSubTypeAttribute.class, 1.0, LocationSubTypeAttribute::new);
        genPatAttrConfig.addAttribute("NameAttribute", NameAttribute.class, 1.0, NameAttribute::new);
        genPatAttrConfig.addAttribute("ExprTypeAttribute", ExprTypeAttribute.class, 1.0, ExprTypeAttribute::new);


        genPatAttrConfig.addAttribute("MoTypeAttribute", MoTypeAttribute.class, 0.5, MoTypeAttribute::new);
        genPatAttrConfig.addAttribute("TokenAttribute", TokenAttribute.class, 0.5, TokenAttribute::new);

        try {
            nameMap = FileUtils.loadGenPatMap(Path.of("05resources/AllTokens_var.txt"));
            apiMap = FileUtils.loadGenPatMap(Path.of("05resources/AllTokens_api.txt"));
            typeMap = FileUtils.loadGenPatMap(Path.of("05resources/AllTokens_type.txt"));
        } catch (IOException e) {
            logger.error("Failed when load token mapping");
            throw new RuntimeException(e);
        }
    }

    public static AttributeConfig getAttrConfig() {
        return genPatAttrConfig;
    }

    private final Set<MoNode> considerNodeCandidates = new HashSet<>();

    @Override
    public boolean shouldConsider(MoNode node) {
        return considerNodeCandidates.contains(node);
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

        MoNode node = attribute.getNode();
        if(attribute instanceof NameAttribute) {
            if (node instanceof MoName name) {
                MoNode parent = name.getParent();
                if(parent instanceof MoMethodInvocation) {
                    return abstraction(name.getIdentifier(), apiMap);
                } else if(parent instanceof MoType) {
                    return abstraction(name.getIdentifier(), typeMap);
                } else {
                    return abstraction(name.getIdentifier(), nameMap);
                }
            } else {
                return false;
            }
        } else if (attribute instanceof ExprTypeAttribute) {
            if(node instanceof MoExpression expr) {
                return abstraction(expr.getExprTypeStr(), typeMap);
            } else {
                return false;
            }
        }
        return false;
    }

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

    /**
     * 频率小于阈值的时候说明这个东西可能比较重要
     * @param token
     * @param map
     * @return
     */
    private boolean abstraction(String token, Map<String, Integer> map) {
        double numInDoc = map.getOrDefault(token, 1);
        double frequency = numInDoc / TOTAL_FILE_NUM;
        return frequency < threshold;
    }
}
