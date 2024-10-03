package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.FieldAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class MoFieldAccess extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoFieldAccess.class);
    @Serial
    private static final long serialVersionUID = -5757462140733848445L;

    private final static Description<MoFieldAccess, MoExpression> expressionDescription =
            new Description<>(ChildType.CHILD, MoFieldAccess.class, MoExpression.class,
                    "expression", true);

    private final static Description<MoFieldAccess, MoSimpleName> nameDescription =
            new Description<>(ChildType.CHILD, MoFieldAccess.class, MoSimpleName.class,
                    "name", true);

    private final static Map<String, Description<MoFieldAccess, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("expression", expressionDescription),
            Map.entry("name", nameDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "expression", mandatory = true)
    private MoExpression expression;

    @RoleDescriptor(type = ChildType.CHILD, role = "name", mandatory = true)
    private MoSimpleName name;

    public MoFieldAccess(Path fileName, int startLine, int endLine, FieldAccess fieldAccess) {
        super(fileName, startLine, endLine, fieldAccess);
        moNodeType = MoNodeType.TYPEFieldAccess;
    }

    public void setExpression(MoExpression expression) {
        this.expression = expression;
    }

    public void setName(MoSimpleName name) {
        this.name = name;
    }

    public MoExpression getExpression() {
        return expression;
    }

    public MoSimpleName getName() {
        return name;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoFieldAccess(this);
    }

    @Override
    public List<MoNode> getChildren() {
        return List.of(expression, name);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoFieldAccess, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            return expression;
        } else if(description == nameDescription) {
            return name;
        } else {
            logger.error("Role {} not found in MoFieldAccess", role);
            return null;
        }

    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoFieldAccess, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            expression = (MoExpression) value;
        } else if(description == nameDescription) {
            name = (MoSimpleName) value;
        } else {
            logger.error("Role {} not found in MoFieldAccess", role);
        }
    }

    public static Map<String, Description<MoFieldAccess, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoFieldAccess(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoFieldAccess otherFieldAccess) {
            return expression.isSame(otherFieldAccess.expression) && name.isSame(otherFieldAccess.name);
        }
        return false;
    }
}
