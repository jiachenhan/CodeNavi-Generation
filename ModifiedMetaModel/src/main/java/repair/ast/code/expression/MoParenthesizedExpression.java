package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.declaration.MoFieldDeclaration;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.Map;

public class MoParenthesizedExpression extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoParenthesizedExpression.class);
    @Serial
    private static final long serialVersionUID = -6890254037039287453L;

    private final static Description<MoParenthesizedExpression, MoExpression> expressionDescription =
            new Description<>(ChildType.CHILD, MoParenthesizedExpression.class, MoExpression.class,
                    "expression", true);

    private final static Map<String, Description<MoParenthesizedExpression, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("expression", expressionDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "expression", mandatory = true)
    private MoExpression expression;

    public MoParenthesizedExpression(String fileName, int startLine, int endLine, ParenthesizedExpression parenthesizedExpression) {
        super(fileName, startLine, endLine, parenthesizedExpression);
        moNodeType = MoNodeType.TYPEParenthesizedExpression;
    }

    public void setExpression(MoExpression expression) {
        this.expression = expression;
    }

    public MoExpression getExpression() {
        return expression;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoParenthesizedExpression(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoParenthesizedExpression, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            return expression;
        } else {
            logger.error("Role {} not found in MoParenthesizedExpression", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoParenthesizedExpression, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            this.expression = (MoExpression) value;
        } else {
            logger.error("Role {} not found in MoParenthesizedExpression", role);
        }
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoParenthesizedExpression(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoParenthesizedExpression otherParenthesizedExpression) {
            return this.expression.isSame(otherParenthesizedExpression.expression);
        }
        return false;
    }
}
