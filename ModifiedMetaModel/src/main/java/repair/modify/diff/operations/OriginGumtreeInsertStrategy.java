package repair.modify.diff.operations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the strategy for index of original gumtree position
 */
public class OriginGumtreeInsertStrategy implements InsertListStrategy {
    private static final Logger logger = LoggerFactory.getLogger(NaiveIndexStrategy.class);

    private final int index;
    public OriginGumtreeInsertStrategy(int index) {
        this.index = index;
    }

    @Override
    public int computeInsertIndex() {
        return index;
    }
}
