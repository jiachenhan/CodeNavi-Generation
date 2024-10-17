package repair.modify.apply.match;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.pattern.Pattern;
import repair.pattern.attr.*;

import java.util.*;
import java.util.stream.Collectors;

import static repair.pattern.AttributeFactory.attrToWeight;

public class Matcher {
    private final static Logger logger = LoggerFactory.getLogger(Matcher.class);


    public static List<MatchInstance> match(Pattern pattern, MoNode left) {
        RoughMapping roughMapping = roughMatch(pattern, left, 0.2);
        List<MatchInstance> instances = new ArrayList<>();
        matchNext(new DualHashBidiMap<>(), roughMapping, 0, new HashSet<>(), 0.0, instances);
        return instances;
    }

    private static void matchNext(BidiMap<MoNode, MoNode> matchedNodeMap, RoughMapping roughMapping, int i,
                                  Set<MoNode> alreadyMatched, double matchSimilarity,  List<MatchInstance> instances) {
        if(instances.size() > 100) {
            return;
        }
        // 有可能pattern的node数量少，或者buggy的node数量少
        int matchNum = Math.min(roughMapping.getRoughMapping().size(), roughMapping.getBuggyNodeSize());
        if(i == matchNum) {
            instances.add(new MatchInstance(new DualHashBidiMap<>(matchedNodeMap), matchSimilarity, true));
        } else {
            MoNode patternNode = (MoNode) roughMapping.getRoughMapping().keySet().toArray()[i];
            List<Pair<MoNode, Double>> leftNodes = roughMapping.getRoughMapping().get(patternNode);
            for (Pair<MoNode, Double> leftNode : leftNodes) {
                if(alreadyMatched.contains(leftNode.getLeft())) {
                    continue;
                }
                if(!checkParentEdge(patternNode, leftNode.getLeft(), matchedNodeMap)) {
                    continue;
                }
                matchedNodeMap.put(patternNode, leftNode.getLeft());
                alreadyMatched.add(leftNode.getLeft());
                matchNext(matchedNodeMap, roughMapping, i+1, alreadyMatched, matchSimilarity + leftNode.getRight(), instances);
                matchedNodeMap.remove(patternNode);
                alreadyMatched.remove(leftNode.getLeft());
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


    /**
     * 粗匹配，筛选出每个pattern节点可以匹配的left节点，返回一个粗匹配结果
     * @param pattern 模式树
     * @param left 待匹配树
     * @param threshold 相似度阈值
     * @return
     */
    public static RoughMapping roughMatch(Pattern pattern, MoNode left, double threshold) {
        RoughMapping roughMapping = new RoughMapping();
        Map<MoNode, Map<Class<? extends Attribute<?>>, Attribute<?>>> leftToAttributes = Attribute.computeAttributes(left);
        roughMapping.setBuggyNodeSize(leftToAttributes.size());
        // 筛选出来需要考虑的节点
        List<MoNode> consideredNodes = pattern.getNodeToConsidered().entrySet().stream().filter(Map.Entry::getValue).map(Map.Entry::getKey).toList();

        Map<MoNode, Map<Class<? extends Attribute<?>>, Attribute<?>>> patternBeforeToAttributes = pattern.getNodeToAttributes().entrySet().stream()
                .filter(entry -> consideredNodes.contains(entry.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // 先对需要考虑属性的节点进行匹配
        for (Map.Entry<MoNode, Map<Class<? extends Attribute<?>>, Attribute<?>>> beforeEntry : patternBeforeToAttributes.entrySet()) {
            MoNode patternBeforeNode = beforeEntry.getKey();
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

    /**
     * 计算两个节点属性之间的相似度
     * 如果两个节点的硬属性不能匹配，则返回-1
     * range: [-1] [0, 1]
     * @param patternBeforeAttributes 模式节点的属性Map
     * @param leftAttributes 待匹配节点的属性Map
     * @return 相似度分数
     */
    private static double computeSimilarity(Map<Class<? extends Attribute<?>>, Attribute<?>> patternBeforeAttributes, Map<Class<? extends Attribute<?>>, Attribute<?>> leftAttributes) {
        double similarity = 0;
        // 用于判断这个节点是不是所有属性都不用考虑(空节点)
        boolean noAttrsConsiderFlag = true;
        for (Map.Entry<Class<? extends Attribute<?>>, Attribute<?>> patternEntry : patternBeforeAttributes.entrySet()) {
            Class<? extends Attribute<?>> attrClass = patternEntry.getKey();
            Attribute<?> patternAttr = patternEntry.getValue();
            if (!patternAttr.isConsidered()) {
                // 该属性不被考虑
                continue;
            }
            noAttrsConsiderFlag = false;

            Attribute<?> leftAttr = leftAttributes.get(attrClass);
            double sim = patternAttr.similarity(leftAttr);
            if(patternAttr instanceof HardConstraint) {
                // 该属性是硬约束, 两个节点必须完全一致
                if(sim == -1.0) {
                    return -1.0;
                }
            } else {
                similarity += leftAttr.similarity(patternAttr) * attrToWeight.get(attrClass);
            }
        }
        if(noAttrsConsiderFlag) {
            // 该节点所有属性都不用考虑
            return 1.0;
        }
        return similarity;
    }


}
