package repair.pattern.attr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;

@RegisterAttr
public class MoTypeAttribute extends Attribute<Class<? extends MoNode>>{
    private static final Logger logger = LoggerFactory.getLogger(MoTypeAttribute.class);

    public MoTypeAttribute(MoNode node) {
        super(node);
        this.value = node.getClass();
        super.considered = true;
    }

    @Override
    public double similarity(Attribute<?> other) {
        if (other instanceof MoTypeAttribute moTypeAttribute) {
            return this.value.equals(moTypeAttribute.value) ? 1 : 0;
        }
        logger.error("Cannot compare MoTypeAttribute with " + other.getClass());
        return -1;
    }
}
