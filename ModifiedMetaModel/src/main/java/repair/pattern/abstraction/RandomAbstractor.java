package repair.pattern.abstraction;

import repair.ast.MoNode;
import repair.pattern.Pattern;
import repair.pattern.attr.Attribute;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

public class RandomAbstractor implements Abstractor{
    @Override
    public boolean shouldConsider(MoNode node) {
        return false;
    }

    @Override
    public boolean shouldConsider(Attribute<?> attribute) {
        return false;
    }

    @Override
    public void doAbstraction(Pattern pattern) {
        Map<MoNode, Boolean> nodeToConsidered = pattern.getNodeToConsidered();
        Map<MoNode, Map<Class<? extends Attribute<?>>, Attribute<?>>> nodeToAttributes = pattern.getNodeToAttributes();

        Random random = new Random(42);
        int numberOfElementsToSelect = (int) (nodeToConsidered.size() * 0.1);
        List<MoNode> randomConsideredNodes = nodeToConsidered.keySet().stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> {
                            Collections.shuffle(list, random);
                            return list.stream().limit(numberOfElementsToSelect).toList();
                        }
                ));

        nodeToConsidered.forEach((node, considered) -> {
            boolean shouldConsideredNode = randomConsideredNodes.contains(node);
            nodeToConsidered.put(node, shouldConsideredNode);

            Map<Class<? extends Attribute<?>>, Attribute<?>> attributes = nodeToAttributes.get(node);
            attributes.forEach((attrClass, attr) -> {
                if (shouldConsideredNode) {
                    attr.setConsidered(shouldConsider(attr));
                } else {
                    attr.setConsidered(false);
                }
            });
        });
    }
}
