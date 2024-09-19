package repair.ast.code.expression.literal;

import org.eclipse.jdt.core.dom.CharacterLiteral;
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
import java.util.List;
import java.util.Map;

public class MoCharacterLiteral extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoCharacterLiteral.class);
    @Serial
    private static final long serialVersionUID = -645257045128926840L;

    private final static Description<MoCharacterLiteral, String> valueDescription =
            new Description<>(ChildType.SIMPLE, MoCharacterLiteral.class, String.class,
                    "escapedValue", true);

    private final static Map<String, Description<MoCharacterLiteral, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("escapedValue", valueDescription)
    );

    @RoleDescriptor(type = ChildType.SIMPLE, role = "escapedValue", mandatory = true)
    private String escapedValue;

    public MoCharacterLiteral(String fileName, int startLine, int endLine, CharacterLiteral characterLiteral) {
        super(fileName, startLine, endLine, characterLiteral);
        moNodeType = MoNodeType.TYPECharacterLiteral;
    }

    public String getEscapedValue() {
        return escapedValue;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoCharacterLiteral(this);
    }


    @Override
    public List<MoNode> getChildren() {
        return List.of();
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoCharacterLiteral, ?> description = descriptionsMap.get(role);
        if(description == valueDescription) {
            return escapedValue;
        } else {
            logger.error("Role {} not found in MoCharacterLiteral", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoCharacterLiteral, ?> description = descriptionsMap.get(role);
        if(description == valueDescription) {
            this.escapedValue = (String) value;
        } else {
            logger.error("Role {} not found in MoCharacterLiteral", role);
            return;
        }
    }

    public static Map<String, Description<MoCharacterLiteral, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoCharacterLiteral clone = new MoCharacterLiteral(getFileName(), getStartLine(), getEndLine(), null);
        clone.setStructuralProperty("escapedValue", getEscapedValue());
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if (other instanceof MoCharacterLiteral characterLiteral) {
            return escapedValue.equals(characterLiteral.escapedValue);
        }
        return false;
    }
}
