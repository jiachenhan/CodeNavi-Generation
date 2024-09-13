package repair.apply.match;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import repair.ast.MoNode;
import repair.pattern.Pattern;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class MatchMock {
    public static MatchInstance match(Pattern pattern, MoNode left, BidiMap<MoNode, MoNode> copyMap) {
        return new MatchInstance(new DualHashBidiMap<>(copyMap), 100, true);
    }
}
