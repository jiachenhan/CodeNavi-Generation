package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoExpression;
import repair.ast.code.type.MoType;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Map;

public class MoWhileStatement extends MoStatement {
    private static final Logger logger = LoggerFactory.getLogger(MoWhileStatement.class);
    @Serial
    private static final long serialVersionUID = -8901969938639750627L;

    private final static Description<MoWhileStatement, MoExpression> expressionDescription =
            new Description<>(ChildType.CHILD, MoWhileStatement.class, MoExpression.class,
                    "expression", true);

    private final static Description<MoWhileStatement, MoStatement> bodyDescription =
            new Description<>(ChildType.CHILD, MoWhileStatement.class, MoStatement.class,
                    "body", true);

    private final static Map<String, Description<MoWhileStatement, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("expression", expressionDescription),
            Map.entry("body", bodyDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "expression", mandatory = true)
    private MoExpression condition;
    @RoleDescriptor(type = ChildType.CHILD, role = "body", mandatory = true)
    private MoStatement body;

    public MoWhileStatement(String fileName, int startLine, int endLine, WhileStatement whileStatement) {
        super(fileName, startLine, endLine, whileStatement);
        moNodeType = MoNodeType.TYPEWhileStatement;
    }

    public void setCondition(MoExpression condition) {
        this.condition = condition;
    }

    public void setBody(MoStatement body) {
        this.body = body;
    }

    public MoExpression getCondition() {
        return condition;
    }

    public MoStatement getBody() {
        return body;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoWhileStatement(this);
    }

    @Override
    public List<MoNode> getChildren() {
        return List.of(condition, body);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoWhileStatement, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            return condition;
        } else if(description == bodyDescription) {
            return body;
        } else {
            logger.error("Role {} not found in MoWhileStatement", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoWhileStatement, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            condition = (MoExpression) value;
        } else if(description == bodyDescription) {
            body = (MoStatement) value;
        } else {
            logger.error("Role {} not found in MoWhileStatement", role);
        }
    }

    public static Map<String, Description<MoWhileStatement, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoWhileStatement(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoWhileStatement otherWhileStatement) {
            return condition.isSame(otherWhileStatement.condition) && body.isSame(otherWhileStatement.body);
        }
        return false;
    }
}
