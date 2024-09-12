package repair.pattern.attr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;

public class LocationSubTypeAttribute extends Attribute<Class<?>> implements HardConstraint {
    private static final Logger logger = LoggerFactory.getLogger(LocationSubTypeAttribute.class);
    public LocationSubTypeAttribute(MoNode node) {
        super(node);
        if(node.getLocationInParent() == null) {
            this.value = null;
        } else {
            this.value = node.getLocationInParent().childNodeType();
        }
        super.considered = true;
    }

    @Override
    public double similarity(Attribute<?> other) {
        if (other instanceof LocationSubTypeAttribute locationSubTypeAttribute) {
            if(this.value == null || locationSubTypeAttribute.value == null) {
                return 0;
            }
            return this.value.isAssignableFrom(node.getClass()) ? 1 : 0;
        }
        logger.error("Cannot compare LocationSubTypeAttribute with " + other.getClass());
        return -1;
    }
}
