package repair.pattern.attr;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.pattern.serialize.rules.LocationSubTypeAtrSerializer;

import java.io.Serial;

/**
 * 标识孩子节点的父类型硬约束能否满足
 */
@RegisterAttr
@JsonSerialize(using = LocationSubTypeAtrSerializer.class)
public class LocationSubTypeAttribute extends Attribute<Class<?>> implements HardConstraint {
    private static final Logger logger = LoggerFactory.getLogger(LocationSubTypeAttribute.class);
    @Serial
    private static final long serialVersionUID = 1404681854826655524L;

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
                return -1;
            }
            return this.value.isAssignableFrom(node.getClass()) ? 1 : -1;
        }
        logger.error("Cannot compare LocationSubTypeAttribute with " + other.getClass());
        return -1;
    }
}
