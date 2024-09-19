package repair.ast.code.virtual;

import org.eclipse.jdt.core.dom.ASTNode;
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

public class MoMethodInvocationTarget extends MoVirtualChildNode {
    private static final Logger logger = LoggerFactory.getLogger(MoMethodInvocationTarget.class);
    @Serial
    private static final long serialVersionUID = 6135110836607656927L;

    private final static Description<MoMethodInvocationTarget, MoExpression> expressionDescription =
            new Description<>(ChildType.CHILD, MoMethodInvocationTarget.class, MoExpression.class,
                    "expression", true);

    private final static Map<String, Description<MoMethodInvocationTarget, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("expression", expressionDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "expression", mandatory = true)
    private MoExpression expression;

    public MoMethodInvocationTarget(String fileName, int startLine, int endLine, int elementPos, int elementLength, ASTNode oriNode) {
        super(fileName, startLine, endLine, elementPos, elementLength, null);
        moNodeType = MoNodeType.TYPEMethodInvocationTarget;
    }

    @Override
    public List<MoNode> getChildren() {
        return List.of(expression);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    public MoExpression getExpression() {
        return expression;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoMethodInvocationTarget, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            return expression;
        } else {
            logger.error("Role {} not found in MoMethodInvocationTarget", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoMethodInvocationTarget, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            expression = (MoExpression) value;
        } else {
            logger.error("Role {} not found in MoMethodInvocationTarget", role);
        }
    }

    public static Map<String, Description<MoMethodInvocationTarget, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoMethodInvocationTarget(getFileName(), getStartLine(), getEndLine(), getElementPos(), getElementLength(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoMethodInvocationTarget moMethodInvocationTarget) {
            return expression.isSame(moMethodInvocationTarget.expression);
        }
        return false;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoMethodInvocationTarget(this);
    }
}
