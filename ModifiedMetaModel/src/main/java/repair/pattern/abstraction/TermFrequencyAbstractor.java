package repair.pattern.abstraction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.FileUtils;
import repair.ast.MoNode;
import repair.pattern.Pattern;
import repair.pattern.attr.Attribute;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TermFrequencyAbstractor implements Abstractor {
    private final static Logger logger = LoggerFactory.getLogger(TermFrequencyAbstractor.class);
    private final static Map<String, Integer> nameMap;
    private final static Map<String, Integer> apiMap;
    private final static Map<String, Integer> typeMap;
    private final static int TOTAL_FILE_NUM = 1217392;
    private final static double threshold = 0.005;

    static {
        try {
            nameMap = FileUtils.loadGenPatMap(Path.of("05resources/AllTokens_var.txt"));
            apiMap = FileUtils.loadGenPatMap(Path.of("05resources/AllTokens_api.txt"));
            typeMap = FileUtils.loadGenPatMap(Path.of("05resources/AllTokens_type.txt"));
        } catch (IOException e) {
            logger.error("Failed when load token mapping");
            throw new RuntimeException(e);
        }
    }

    private List<MoNode> actionsRelatedNodes = new ArrayList<>();

    private final List<MoNode> considerNodeCandidates = new ArrayList<>();

    @Override
    public boolean shouldConsider(MoNode node) {
        return considerNodeCandidates.contains(node);
    }

    @Override
    public boolean shouldConsider(Attribute<?> attribute) {
        return false;
    }

    @Override
    public void doAbstraction(Pattern pattern) {
        Map<MoNode, Boolean> nodeToConsidered = pattern.getNodeToConsidered();
        Map<MoNode, Map<Class<? extends Attribute<?>>, Attribute<?>>> nodeToAttributes = pattern.getNodeToAttributes();

        // get action related
        actionsRelatedNodes = getActionRelatedNodes(pattern);
        // expand action nodes
        // todo: update considerCandidates


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
}
