package repair.pattern.attr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.code.expression.MoName;

public class NameAttribute extends Attribute<String> implements HardConstraint {
    private static final Logger logger = LoggerFactory.getLogger(NameAttribute.class);

    public NameAttribute(MoNode node) {
        super(node);
        if(node instanceof MoName name) {
            this.value = name.getIdentifier();
        } else {
            this.value = "<UNCompatible>";
        }
        super.considered = true;
    }

    @Override
    public double similarity(Attribute<?> other) {
        if (other instanceof NameAttribute nameAttribute) {
            return this.value.equals(nameAttribute.value) ? 1 : -1;
        }
        logger.error("Cannot compare NameAttribute with " + other.getClass());
        return -1;
    }
}
