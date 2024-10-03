package repair.ast.code.expression.literal;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoExpression;
import repair.ast.declaration.MoAnonymousClassDeclaration;
import repair.ast.declaration.MoBodyDeclaration;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class MoBooleanLiteral extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoBooleanLiteral.class);
    @Serial
    private static final long serialVersionUID = -7539678407458399626L;

    private final static Description<MoBooleanLiteral, Boolean> booleanValueDescription =
            new Description<>(ChildType.SIMPLE, MoBooleanLiteral.class, Boolean.class,
                    "booleanValue", true);

    private final static Map<String, Description<MoBooleanLiteral, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("booleanValue", booleanValueDescription)
    );

    @RoleDescriptor(type = ChildType.SIMPLE, role = "booleanValue", mandatory = true)
    private boolean value;

    public MoBooleanLiteral(Path fileName, int startLine, int endLine, BooleanLiteral booleanLiteral) {
        super(fileName, startLine, endLine, booleanLiteral);
        moNodeType = MoNodeType.TYPEBooleanLiteral;
    }

    public boolean getValue() {
        return value;
    }

    @Override
    public MoNodeType getMoNodeType() {
        return super.getMoNodeType();
    }

    @Override
    public MoNode shallowClone() {
        MoBooleanLiteral clone = new MoBooleanLiteral(getFileName(), getStartLine(), getEndLine(), null);
        clone.setStructuralProperty("booleanValue", getValue());
        return clone;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoBooleanLiteral(this);
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
        Description<MoBooleanLiteral, ?> description = descriptionsMap.get(role);
        if(description == booleanValueDescription) {
            return value;
        } else {
            logger.error("Role {} not found in MoAnonymousClassDeclaration", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoBooleanLiteral, ?> description = descriptionsMap.get(role);
        if(description == booleanValueDescription) {
            this.value = (boolean) value;
        } else {
            logger.error("Role {} not found in MoAnonymousClassDeclaration", role);
            return;
        }
    }

    public static Map<String, Description<MoBooleanLiteral, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoBooleanLiteral moBooleanLiteral) {
            return this.value == moBooleanLiteral.value;
        }
        return false;
    }
}
