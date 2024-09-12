package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoExpression;
import repair.ast.declaration.MoSingleVariableDeclaration;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.Map;

public class MoEnhancedForStatement extends MoStatement {
    private static final Logger logger = LoggerFactory.getLogger(MoEnhancedForStatement.class);
    @Serial
    private static final long serialVersionUID = 5111275786766098471L;

    private final static Description<MoEnhancedForStatement, MoSingleVariableDeclaration> parameterDescription =
            new Description<>(ChildType.CHILD, MoEnhancedForStatement.class, MoSingleVariableDeclaration.class,
                    "parameter", true);

    private final static Description<MoEnhancedForStatement, MoExpression> expressionDescription =
            new Description<>(ChildType.CHILD, MoEnhancedForStatement.class, MoExpression.class,
                    "expression", true);

    private final static Description<MoEnhancedForStatement, MoStatement> bodyDescription =
            new Description<>(ChildType.CHILD, MoEnhancedForStatement.class, MoStatement.class,
                    "body", true);

    private final static Map<String, Description<MoEnhancedForStatement, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("parameter", parameterDescription),
            Map.entry("expression", expressionDescription),
            Map.entry("body", bodyDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "parameter", mandatory = true)
    private MoSingleVariableDeclaration parameter;
    @RoleDescriptor(type = ChildType.CHILD, role = "expression", mandatory = true)
    private MoExpression expression;
    @RoleDescriptor(type = ChildType.CHILD, role = "body", mandatory = true)
    private MoStatement body;

    public MoEnhancedForStatement(String fileName, int startLine, int endLine, EnhancedForStatement enhancedForStatement) {
        super(fileName, startLine, endLine, enhancedForStatement);
        moNodeType = MoNodeType.TYPEEnhancedForStatement;
    }

    public void setParameter(MoSingleVariableDeclaration parameter) {
        this.parameter = parameter;
    }

    public void setExpression(MoExpression expression) {
        this.expression = expression;
    }

    public void setBody(MoStatement body) {
        this.body = body;
    }

    public MoSingleVariableDeclaration getParameter() {
        return parameter;
    }

    public MoExpression getExpression() {
        return expression;
    }

    public MoStatement getBody() {
        return body;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoEnhancedForStatement(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoEnhancedForStatement, ?> description = descriptionsMap.get(role);
        if(description == parameterDescription) {
            return parameter;
        } else if(description == expressionDescription) {
            return expression;
        } else if(description == bodyDescription) {
            return body;
        } else {
            logger.error("Role {} not found in MoEnhancedForStatement", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoEnhancedForStatement, ?> description = descriptionsMap.get(role);
        if(description == parameterDescription) {
            this.parameter = (MoSingleVariableDeclaration) value;
        } else if(description == expressionDescription) {
            this.expression = (MoExpression) value;
        } else if(description == bodyDescription) {
            this.body = (MoStatement) value;
        } else {
            logger.error("Role {} not found in MoEnhancedForStatement", role);
        }
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoEnhancedForStatement(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoEnhancedForStatement moEnhancedForStatement) {
            boolean match = parameter.isSame(moEnhancedForStatement.parameter);
            match = match && expression.isSame(moEnhancedForStatement.expression);
            match = match && body.isSame(moEnhancedForStatement.body);
            return match;
        }
        return false;
    }
}
