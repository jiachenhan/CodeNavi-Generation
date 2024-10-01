package repair.pattern.attr;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.code.expression.MoName;
import repair.ast.code.expression.MoQualifiedName;
import repair.ast.code.expression.MoSimpleName;
import repair.pattern.serialize.rules.NameAttrSerializer;

import java.io.Serial;

@RegisterAttr
@JsonSerialize(using = NameAttrSerializer.class)
public class NameAttribute extends Attribute<String> implements HardConstraint {
    private static final Logger logger = LoggerFactory.getLogger(NameAttribute.class);
    @Serial
    private static final long serialVersionUID = -7311698286502605500L;

    public NameAttribute(MoNode node) {
        super(node);
        if(node instanceof MoSimpleName simpleName) {
            this.value = simpleName.getIdentifier();
        } else if (node instanceof MoQualifiedName qualifiedName) {
            this.value = qualifiedName.toSrcString();
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
