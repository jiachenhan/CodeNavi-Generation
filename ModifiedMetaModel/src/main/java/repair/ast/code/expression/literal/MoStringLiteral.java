package repair.ast.code.expression.literal;

import org.eclipse.jdt.core.dom.StringLiteral;
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

public class MoStringLiteral extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoStringLiteral.class);
    @Serial
    private static final long serialVersionUID = 8055730085118546326L;
    private final static Description<MoStringLiteral, String> valueDescription =
            new Description<>(ChildType.SIMPLE, MoStringLiteral.class, String.class,
                    "escapedValue", true);

    private final static Map<String, Description<MoStringLiteral, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("escapedValue", valueDescription)
    );

    @RoleDescriptor(type = ChildType.SIMPLE, role = "escapedValue", mandatory = true)
    private String escapedValue;

    public MoStringLiteral(String fileName, int startLine, int endLine, StringLiteral stringLiteral) {
        super(fileName, startLine, endLine, stringLiteral);
        moNodeType = MoNodeType.TYPEStringLiteral;
    }

    public String getEscapedValue() {
        return escapedValue;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoStringLiteral(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoStringLiteral, ?> description = descriptionsMap.get(role);
        if(description == valueDescription) {
            return escapedValue;
        } else {
            logger.error("Role {} not found in MoStringLiteral", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoStringLiteral, ?> description = descriptionsMap.get(role);
        if(description == valueDescription) {
            this.escapedValue = (String) value;
        } else {
            logger.error("Role {} not found in MoStringLiteral", role);
        }
    }

    public static Map<String, Description<MoStringLiteral, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoStringLiteral clone = new MoStringLiteral(getFileName(), getStartLine(), getEndLine(), null);
        clone.setStructuralProperty("escapedValue", getEscapedValue());
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoStringLiteral moStringLiteral) {
            return escapedValue.equals(moStringLiteral.escapedValue);
        }
        return false;
    }
}
