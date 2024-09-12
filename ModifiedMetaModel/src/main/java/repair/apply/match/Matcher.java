package repair.apply.match;

import com.github.gumtreediff.utils.Pair;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.visitor.FlattenScanner;
import repair.pattern.AttributeFactory;
import repair.pattern.Pattern;
import repair.pattern.attr.*;

import java.util.*;

public class Matcher {
    private final static Logger logger = LoggerFactory.getLogger(Matcher.class);

    private static final Map<Class<? extends Attribute<?>>, Double> attrToWeight = new HashMap<>();
    static {
        attrToWeight.put(LocationSubTypeAttribute.class, 1.0); // weight is not used for hard constraints

        attrToWeight.put(MoTypeAttribute.class, 0.5);
        attrToWeight.put(TokenAttribute.class, 0.5);
    }

    public static Map<MoNode, Map<Class<? extends Attribute<?>>, Attribute<?>>> computeAttributes(MoNode left) {
        Map<MoNode, Map<Class<? extends Attribute<?>>, Attribute<?>>> nodeToAttributes = new HashMap<>();
        for (MoNode leftNode : new FlattenScanner().flatten(left)) {
            Map<Class<? extends Attribute<?>>, Attribute<?>> attributes = AttributeFactory.createAttributes(leftNode);
            nodeToAttributes.put(leftNode, attributes);
        }
        return nodeToAttributes;
    }

    public static List<MatchInstance> match(Pattern pattern, MoNode left) {
        RoughMapping roughMapping = roughMatch(pattern, left, 0.5);
        List<MatchInstance> instances = new ArrayList<>();
        matchNext(new DualHashBidiMap<>(), roughMapping, 0, new HashSet<>(), 0.0, instances);
        return instances;
    }

    private static void matchNext(BidiMap<MoNode, MoNode> matchedNodeMap, RoughMapping roughMapping, int i,
                                  Set<MoNode> alreadyMatched, double matchSimilarity,  List<MatchInstance> instances) {
        if(instances.size() > 100) {
            return;
        }
        if(i == roughMapping.getRoughMapping().size()) {
            instances.add(new MatchInstance(new DualHashBidiMap<>(matchedNodeMap), matchSimilarity, true));
        } else {
            MoNode patternNode = (MoNode) roughMapping.getRoughMapping().keySet().toArray()[i];
            List<Pair<MoNode, Double>> leftNodes = roughMapping.getRoughMapping().get(patternNode);
            for (Pair<MoNode, Double> leftNode : leftNodes) {
                if(alreadyMatched.contains(leftNode.first)) {
                    continue;
                }
                if(!checkParentEdge(patternNode, leftNode.first, matchedNodeMap)) {
                    continue;
                }
                matchedNodeMap.put(patternNode, leftNode.first);
                alreadyMatched.add(leftNode.first);
                matchNext(matchedNodeMap, roughMapping, i+1, alreadyMatched, matchSimilarity + leftNode.second, instances);
                matchedNodeMap.remove(patternNode);
                alreadyMatched.remove(leftNode.first);
            }
        }
    }

    /**
     * 检查父节点的边是否合法， 这里只有比较宽松的约束，这种匹配方式可能出现子节点匹配的节点和父节点不连续的问题
     * todo: 模式子树匹配
     * @param beforeNode 需要匹配的pattern节点
     * @param leftNode 需要匹配的left节点
     * @param track 已经匹配的节点map
     * @return 是否合法
     */
    private static boolean checkParentEdge(MoNode beforeNode, MoNode leftNode, BidiMap<MoNode, MoNode> track) {
        MoNode beforeParent = beforeNode.getParent();
        MoNode leftParent = leftNode.getParent();
        if(beforeParent != null && leftParent != null) {
            MoNode beforeParentBindLeft = track.get(beforeParent);
            MoNode leftParentBindBefore = track.getKey(leftParent);

            if(beforeParentBindLeft != null && leftParentBindBefore != null &&
                    ! beforeParentBindLeft.equals(leftParent) && !leftParentBindBefore.equals(beforeParent)) {
                return false;
            }
        }
        return true;
    }


    public static RoughMapping roughMatch(Pattern pattern, MoNode left, double threshold) {
        RoughMapping roughMapping = new RoughMapping();
        Map<MoNode, Map<Class<? extends Attribute<?>>, Attribute<?>>> leftToAttributes = computeAttributes(left);
        Map<MoNode, Map<Class<? extends Attribute<?>>, Attribute<?>>> patternBeforeToAttributes = pattern.getNodeToAttributes();

        for (Map.Entry<MoNode, Map<Class<? extends Attribute<?>>, Attribute<?>>> beforeEntry : patternBeforeToAttributes.entrySet()) {
            MoNode patternBeforeNode = beforeEntry.getKey();
            logger.debug("Matching pattern before node {}", patternBeforeNode);
            Map<Class<? extends Attribute<?>>, Attribute<?>> patternBeforeAttributes = beforeEntry.getValue();

            for (Map.Entry<MoNode, Map<Class<? extends Attribute<?>>, Attribute<?>>> leftEntry : leftToAttributes.entrySet()) {
                MoNode leftNode = leftEntry.getKey();
                Map<Class<? extends Attribute<?>>, Attribute<?>> leftAttributes = leftEntry.getValue();

                double similarity = computeSimilarity(patternBeforeAttributes, leftAttributes);
                if(similarity == -1) {
                    // 该匹配不合法
                    continue;
                } else {
                    roughMapping.addMapping(patternBeforeNode, leftNode, similarity);
                }
            }
        }

        roughMapping.filterMapping(threshold);
        roughMapping.sortMapping();
        return roughMapping;
    }



    private static double computeSimilarity(Map<Class<? extends Attribute<?>>, Attribute<?>> patternBeforeAttributes, Map<Class<? extends Attribute<?>>, Attribute<?>> leftAttributes) {
        double similarity = 0;
        // 用于判断这个节点是不是所有属性都不用考虑(空节点)
        boolean emptyNodeFlag = true;
        for (Map.Entry<Class<? extends Attribute<?>>, Attribute<?>> leftEntry : leftAttributes.entrySet()) {
            Class<? extends Attribute<?>> attrClass = leftEntry.getKey();
            Attribute<?> leftAttr = leftEntry.getValue();
            Attribute<?> patternBeforeAttr = patternBeforeAttributes.get(attrClass);
            if(!patternBeforeAttr.isConsidered()) {
                // 该属性不被考虑
                logger.debug("Attribute {} is not considered", attrClass.getSimpleName());
                continue;
            }
            emptyNodeFlag = false;
            double sim = leftAttr.similarity(patternBeforeAttr);
            if(patternBeforeAttr instanceof HardConstraint) {
                // 该属性是硬约束
                if(sim == -1.0) {
                    return -1.0;
                }
            } else {
                similarity += leftAttr.similarity(patternBeforeAttr) * attrToWeight.get(attrClass);
            }
        }
        if(emptyNodeFlag) {
            // 该节点所有属性都不用考虑
            return 1.0;
        }
        return similarity;
    }


}
