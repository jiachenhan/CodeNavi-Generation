package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.declaration.MoAnonymousClassDeclaration;
import repair.ast.declaration.MoBodyDeclaration;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Map;

public class MoArrayInitializer extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoArrayInitializer.class);
    @Serial
    private static final long serialVersionUID = -9055273786836775172L;

    private final static Description<MoArrayInitializer, MoExpression> expressionDescription =
            new Description<>(ChildType.CHILDLIST, MoArrayInitializer.class, MoExpression.class,
                    "expressions", true);

    private final static Map<String, Description<MoArrayInitializer, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("expressions", expressionDescription)
    );

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "expressions", mandatory = true)
    private final MoNodeList<MoExpression> expressions;

    public MoArrayInitializer(String fileName, int startLine, int endLine, ArrayInitializer arrayInitializer) {
        super(fileName, startLine, endLine, arrayInitializer);
        moNodeType = MoNodeType.TYPEArrayInitializer;
        expressions = new MoNodeList<>(this, expressionDescription);
    }

    public void addExpression(MoExpression expression) {
        expressions.add(expression);
    }

    public List<MoExpression> getExpressions() {
        return expressions;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoArrayInitializer(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoArrayInitializer, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            return expressions;
        } else {
            logger.error("Role {} not found in MoArrayInitializer", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoArrayInitializer, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            expressions.clear();
            expressions.addAll((List<MoExpression>) value);
        } else {
            logger.error("Role {} not found in MoArrayInitializer", role);
        }
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoArrayInitializer(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if (other instanceof MoArrayInitializer arrayInitializer) {
            return MoNodeList.sameList(expressions, arrayInitializer.expressions);
        }
        return false;
    }
}
