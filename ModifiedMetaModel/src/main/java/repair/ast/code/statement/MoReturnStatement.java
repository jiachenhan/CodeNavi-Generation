package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.ReturnStatement;
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
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoReturnStatement extends MoStatement {
    private static final Logger logger = LoggerFactory.getLogger(MoReturnStatement.class);
    @Serial
    private static final long serialVersionUID = 6024851036603244107L;

    private final static Description<MoReturnStatement, MoExpression> expressionDescription =
            new Description<>(ChildType.CHILD, MoReturnStatement.class, MoExpression.class,
                    "expression", false);

    private final static Map<String, Description<MoReturnStatement, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("expression", expressionDescription)
    );


    @RoleDescriptor(type = ChildType.CHILD, role = "expression", mandatory = false)
    private MoExpression expression;

    public MoReturnStatement(Path fileName, int startLine, int endLine, ReturnStatement returnStatement) {
        super(fileName, startLine, endLine, returnStatement);
        moNodeType = MoNodeType.TYPEReturnStatement;
    }

    public void setExpression(MoExpression expression) {
        this.expression = expression;
    }

    public Optional<MoExpression> getExpression() {
        return Optional.ofNullable(expression);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoReturnStatement(this);
    }

    @Override
    public List<MoNode> getChildren() {
        if(expression != null) {
            return List.of(expression);
        }
        return List.of();
    }

    @Override
    public boolean isLeaf() {
        return expression == null;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoReturnStatement, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            return expression;
        } else {
            logger.error("Role {} not found in MoReturnStatement", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoReturnStatement, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            this.expression = (MoExpression) value;
        } else {
            logger.error("Role {} not found in MoReturnStatement", role);
        }
    }

    public static Map<String, Description<MoReturnStatement, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoReturnStatement(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoReturnStatement otherReturnStatement) {
            if(this.expression == null && otherReturnStatement.expression == null) {
                return true;
            } else if(this.expression != null && otherReturnStatement.expression != null) {
                return this.expression.isSame(otherReturnStatement.expression);
            }
        }
        return false;
    }
}
