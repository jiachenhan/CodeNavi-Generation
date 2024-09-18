package repair.modify.apply.match;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;

public class MatchInstance {
    private final static Logger logger = LoggerFactory.getLogger(MatchInstance.class);
    private final boolean isLegal;
    private double matchSimilarity = 0;
    private final BidiMap<MoNode, MoNode> nodeMap = new DualHashBidiMap<>();

    public MatchInstance(BidiMap<MoNode, MoNode> nodeMap, double matchSimilarity, boolean isLegal) {
        this.matchSimilarity = matchSimilarity;
        this.nodeMap.putAll(nodeMap);
        this.isLegal = isLegal;
    }

    public void addNodeMap(MoNode left, MoNode right) {
        if(nodeMap.containsKey(left) || nodeMap.containsValue(right)) {
            logger.error("Node already exists in the map");
            return;
        }
        nodeMap.put(left, right);
    }

    public boolean isLegal() {
        return isLegal;
    }

    public BidiMap<MoNode, MoNode> getNodeMap() {
        return nodeMap;
    }

    public double getMatchSimilarity() {
        return matchSimilarity;
    }



}
