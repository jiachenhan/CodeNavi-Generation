package repair.modify.apply.match;

import com.github.gumtreediff.utils.Pair;
import repair.ast.MoNode;

import java.util.*;

public class RoughMapping {
    private int buggyNodeSize = 0;
    private Map<MoNode, List<Pair<MoNode, Double>>> roughMapping = new LinkedHashMap<>();

    public void setBuggyNodeSize(int buggyNodeSize) {
        this.buggyNodeSize = buggyNodeSize;
    }

    public int getBuggyNodeSize() {
        return buggyNodeSize;
    }

    public void addMapping(MoNode patternNode, MoNode leftNode, double similarity) {
        if (!roughMapping.containsKey(patternNode)) {
            roughMapping.put(patternNode, new ArrayList<>());
        }
        roughMapping.get(patternNode).add(new Pair<>(leftNode, similarity));
    }

    public void sortMapping() {
        for (Map.Entry<MoNode, List<Pair<MoNode, Double>>> entry : roughMapping.entrySet()) {
            List<Pair<MoNode, Double>> list = entry.getValue();
            list.sort((p1, p2) -> Double.compare(p2.second, p1.second)); // 从大到小排序
        }

        List<Map.Entry<MoNode, List<Pair<MoNode, Double>>>> entryList = new ArrayList<>(roughMapping.entrySet());
        entryList.sort(Comparator.comparingInt(e -> e.getValue().size()));

        Map<MoNode, List<Pair<MoNode, Double>>> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<MoNode, List<Pair<MoNode, Double>>> entry : entryList) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        roughMapping = sortedMap;
    }

    public void filterMapping(double threshold) {
        for (Map.Entry<MoNode, List<Pair<MoNode, Double>>> entry : roughMapping.entrySet()) {
            entry.getValue().removeIf(pair -> pair.second <= threshold);
        }
    }

    public Map<MoNode, List<Pair<MoNode, Double>>> getRoughMapping() {
        return roughMapping;
    }

}
