package repair.modify.diff.operations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;

/**
 * the strategy for index of original gumtree position
 */
public class OriginGumtreeInsertStrategy implements InsertListStrategy, Serializable {
    private static final Logger logger = LoggerFactory.getLogger(NaiveIndexStrategy.class);
    @Serial
    private static final long serialVersionUID = 5420241799387289949L;

    private final int index;
    public OriginGumtreeInsertStrategy(int index) {
        this.index = index;
    }

    @Override
    public int computeInsertIndex() {
        return index;
    }
}
