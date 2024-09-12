package repair.pattern.attr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.apply.match.MatchAlgorithm;
import repair.ast.MoNode;

import java.util.List;
import java.util.Map;

@RegisterAttr
public class TokenAttribute extends Attribute<List<String>> {
    private static final Logger logger = LoggerFactory.getLogger(TokenAttribute.class);
    public TokenAttribute(MoNode node) {
        super(node);
        this.value = node.tokens();
        super.considered = true;
    }

    @Override
    public double similarity(Attribute<?> other) {
        if (other instanceof TokenAttribute tokenAttribute) {
            Map<Integer, Integer> lcsMatch = MatchAlgorithm.LCSMatch(this.value, tokenAttribute.value, (o1, o2) -> o1.equals(o2) ? 1 : 0);
            return (lcsMatch.size() * 2.0) / (double) (this.value.size() + tokenAttribute.value.size());
        }
        logger.error("Cannot compare TokenAttribute with " + other.getClass());
        return -1;
    }

}
