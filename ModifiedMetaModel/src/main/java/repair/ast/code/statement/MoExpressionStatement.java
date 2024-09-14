package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoCompilationUnit;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoExpression;
import repair.ast.declaration.MoPackageDeclaration;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.Map;

public class MoExpressionStatement extends MoStatement {
    private static final Logger logger = LoggerFactory.getLogger(MoExpressionStatement.class);
    @Serial
    private static final long serialVersionUID = -9091354195722123488L;

    private final static Description<MoExpressionStatement, MoExpression> expressionDescription =
            new Description<>(ChildType.CHILD, MoExpressionStatement.class, MoExpression.class,
                    "expression", true);

    private final static Map<String, Description<MoExpressionStatement, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("expression", expressionDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "expression", mandatory = true)
    private MoExpression expression;

    public MoExpressionStatement(String fileName, int startLine, int endLine, ExpressionStatement expressionStatement) {
        super(fileName, startLine, endLine, expressionStatement);
        moNodeType = MoNodeType.TYPEExpressionStatement;
    }

    public void setExpression(MoExpression expression) {
        this.expression = expression;
    }

    public MoExpression getExpression() {
        return expression;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoExpressionStatement(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoExpressionStatement, ?> description = descriptionsMap.get(role);
        if (description == expressionDescription) {
            return expression;
        } else {
            logger.error("Role {} not found in MoExpressionStatement", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoExpressionStatement, ?> description = descriptionsMap.get(role);
        if (description == expressionDescription) {
            expression = (MoExpression) value;
        } else {
            logger.error("Role {} not found in MoExpressionStatement", role);
        }
    }

    public static Map<String, Description<MoExpressionStatement, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoExpressionStatement(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if (other instanceof MoExpressionStatement otherStatement) {
            return expression.isSame(otherStatement.expression);
        }
        return false;
    }
}
