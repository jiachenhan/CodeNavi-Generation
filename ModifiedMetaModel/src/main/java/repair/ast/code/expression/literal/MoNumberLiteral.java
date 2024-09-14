package repair.ast.code.expression.literal;

import org.eclipse.jdt.core.dom.NumberLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoExpression;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.Map;

public class MoNumberLiteral extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoNumberLiteral.class);
    @Serial
    private static final long serialVersionUID = -8623541652312147086L;
    private final static Description<MoNumberLiteral, String> valueDescription =
            new Description<>(ChildType.SIMPLE, MoNumberLiteral.class, String.class,
                    "token", true);

    private final static Map<String, Description<MoNumberLiteral, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("token", valueDescription)
    );

    @RoleDescriptor(type = ChildType.SIMPLE, role = "token", mandatory = true)
    private String value;

    public MoNumberLiteral(String fileName, int startLine, int endLine, NumberLiteral numberLiteral) {
        super(fileName, startLine, endLine, numberLiteral);
        moNodeType = MoNodeType.TYPENumberLiteral;
    }

    public String getValue() {
        return value;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoNumberLiteral(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoNumberLiteral, ?> description = descriptionsMap.get(role);
        if(description == valueDescription) {
            return value;
        } else {
            logger.error("Role {} not found in MoNumberLiteral", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoNumberLiteral, ?> description = descriptionsMap.get(role);
        if(description == valueDescription) {
            this.value = (String) value;
        } else {
            logger.error("Role {} not found in MoNumberLiteral", role);
        }
    }

    public static Map<String, Description<MoNumberLiteral, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoNumberLiteral clone = new MoNumberLiteral(getFileName(), getStartLine(), getEndLine(), null);
        clone.setStructuralProperty("token", getValue());
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoNumberLiteral otherNumberLiteral) {
            return this.value.equals(otherNumberLiteral.value);
        }
        return false;
    }
}
