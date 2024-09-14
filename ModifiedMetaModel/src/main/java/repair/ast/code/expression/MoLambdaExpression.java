package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.LambdaExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.declaration.MoVariableDeclaration;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Map;

public class MoLambdaExpression extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoLambdaExpression.class);
    @Serial
    private static final long serialVersionUID = -749518993943883179L;

    private final static Description<MoLambdaExpression, Boolean> parenthesesDescription =
            new Description<>(ChildType.SIMPLE, MoLambdaExpression.class, Boolean.class,
                    "parentheses", true);

    private final static Description<MoLambdaExpression, MoVariableDeclaration> parametersDescription =
            new Description<>(ChildType.CHILDLIST, MoLambdaExpression.class, MoVariableDeclaration.class,
                    "parameters", true);

    private final static Description<MoLambdaExpression, MoNode> bodyDescription =
            new Description<>(ChildType.CHILD, MoLambdaExpression.class, MoNode.class,
                    "body", true);

    private final static Map<String, Description<MoLambdaExpression, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("parentheses", parenthesesDescription),
            Map.entry("parameters", parametersDescription),
            Map.entry("body", bodyDescription)
    );

    @RoleDescriptor(type = ChildType.SIMPLE, role = "parentheses", mandatory = true)
    private boolean parentheses;
    @RoleDescriptor(type = ChildType.CHILDLIST, role = "parameters", mandatory = true)
    private final MoNodeList<MoVariableDeclaration> parameters;

    // should be a block or an expression
    @RoleDescriptor(type = ChildType.CHILD, role = "body", mandatory = true)
    private MoNode body;

    public MoLambdaExpression(String fileName, int startLine, int endLine, LambdaExpression lambdaExpression) {
        super(fileName, startLine, endLine, lambdaExpression);
        moNodeType = MoNodeType.TYPELambdaExpression;
        parameters = new MoNodeList<>(this, parametersDescription);
    }

    public void addParameter(MoVariableDeclaration parameter) {
        parameters.add(parameter);
    }

    public void setBody(MoNode body) {
        this.body = body;
    }

    public boolean hasParentheses() {
        return parentheses;
    }

    public List<MoVariableDeclaration> getParameters() {
        return parameters;
    }

    public MoNode getBody() {
        return body;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoLambdaExpression(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoLambdaExpression, ?> description = descriptionsMap.get(role);
        if(description == parenthesesDescription) {
            return parentheses;
        } else if(description == parametersDescription) {
            return parameters;
        } else if(description == bodyDescription) {
            return body;
        } else {
            logger.error("Role {} not found in MoLambdaExpression", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoLambdaExpression, ?> description = descriptionsMap.get(role);
        if(description == parenthesesDescription) {
            parentheses = (boolean) value;
        } else if(description == parametersDescription) {
            parameters.clear();
            parameters.addAll((List<MoVariableDeclaration>) value);
        } else if(description == bodyDescription) {
            body = (MoNode) value;
        } else {
            logger.error("Role {} not found in MoLambdaExpression", role);
        }
    }

    public static Map<String, Description<MoLambdaExpression, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoLambdaExpression clone = new MoLambdaExpression(getFileName(), getStartLine(), getEndLine(), null);
        clone.setStructuralProperty("parentheses", hasParentheses());
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoLambdaExpression otherLambdaExpression) {
            return parentheses == otherLambdaExpression.parentheses &&
                    MoNodeList.sameList(parameters, otherLambdaExpression.parameters) &&
                    body.isSame(otherLambdaExpression.body);
        }
        return false;
    }
}
