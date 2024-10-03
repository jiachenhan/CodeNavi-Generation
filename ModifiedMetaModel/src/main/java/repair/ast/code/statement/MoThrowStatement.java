package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoExpression;
import repair.ast.code.expression.MoName;
import repair.ast.declaration.MoFieldDeclaration;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class MoThrowStatement extends MoStatement {
    private static final Logger logger = LoggerFactory.getLogger(MoThrowStatement.class);
    @Serial
    private static final long serialVersionUID = 9210911174971446682L;

    private final static Description<MoThrowStatement, MoExpression> expressionDescription =
            new Description<>(ChildType.CHILD, MoThrowStatement.class, MoExpression.class,
                    "expression", true);

    private final static Map<String, Description<MoThrowStatement, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("expression", expressionDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "expression", mandatory = true)
    private MoExpression expression;

    public MoThrowStatement(Path fileName, int startLine, int endLine, ThrowStatement throwStatement) {
        super(fileName, startLine, endLine, throwStatement);
        moNodeType = MoNodeType.TYPEThrowStatement;
    }
    public void setExpression(MoExpression expression) {
        this.expression = expression;
    }

    public MoExpression getExpression() {
        return expression;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoThrowStatement(this);
    }

    @Override
    public List<MoNode> getChildren() {
        return List.of(expression);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoThrowStatement, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            return expression;
        } else {
            logger.error("Role {} not found in MoThrowStatement", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoThrowStatement, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            expression = (MoExpression) value;
        } else {
            logger.error("Role {} not found in MoThrowStatement", role);
        }
    }

    public static Map<String, Description<MoThrowStatement, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoThrowStatement(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoThrowStatement otherThrowStatement) {
            return expression.isSame(otherThrowStatement.expression);
        }
        return false;
    }
}
