package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.type.MoType;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.Map;

public class MoExpressionMethodReference extends MoMethodReference {
    private static final Logger logger = LoggerFactory.getLogger(MoExpressionMethodReference.class);
    @Serial
    private static final long serialVersionUID = 5333536656017546079L;

    private final static Description<MoExpressionMethodReference, MoType> typeArgumentsDescription =
            new Description<>(ChildType.CHILDLIST, MoExpressionMethodReference.class, MoType.class,
                    "typeArguments", true);

    private final static Description<MoExpressionMethodReference, MoExpression> expressionDescription =
            new Description<>(ChildType.CHILD, MoExpressionMethodReference.class, MoExpression.class,
                    "expression", true);

    private final static Description<MoExpressionMethodReference, MoSimpleName> nameDescription =
            new Description<>(ChildType.CHILD, MoExpressionMethodReference.class, MoSimpleName.class,
                    "name", true);

    private final static Map<String, Description<MoExpressionMethodReference, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("typeArguments", typeArgumentsDescription),
            Map.entry("expression", expressionDescription),
            Map.entry("name", nameDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "expression", mandatory = true)
    private MoExpression expression;
    @RoleDescriptor(type = ChildType.CHILD, role = "name", mandatory = true)
    private MoSimpleName simpleName;

    public MoExpressionMethodReference(String fileName, int startLine, int endLine, ExpressionMethodReference expressionMethodReference) {
        super(fileName, startLine, endLine, expressionMethodReference);
        moNodeType = MoNodeType.TYPEExpressionMethodReference;
        super.typeArguments = new MoNodeList<>(this, typeArgumentsDescription);
    }

    public void setExpression(MoExpression expression) {
        this.expression = expression;
    }

    public void setSimpleName(MoSimpleName simpleName) {
        this.simpleName = simpleName;
    }

    public MoExpression getExpression() {
        return expression;
    }

    public MoSimpleName getSimpleName() {
        return simpleName;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoExpressionMethodReference(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoExpressionMethodReference, ?> description = descriptionsMap.get(role);
        if(description == typeArgumentsDescription) {
            return super.typeArguments;
        } else if(description == expressionDescription) {
            return expression;
        } else if(description == nameDescription) {
            return simpleName;
        } else {
            logger.error("Role {} not found in MoExpressionMethodReference", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoExpressionMethodReference, ?> description = descriptionsMap.get(role);
        if(description == typeArgumentsDescription) {
            super.typeArguments.clear();
            super.typeArguments.addAll((MoNodeList<MoType>) value);
        } else if(description == expressionDescription) {
            this.expression = (MoExpression) value;
        } else if(description == nameDescription) {
            this.simpleName = (MoSimpleName) value;
        } else {
            logger.error("Role {} not found in MoExpressionMethodReference", role);
        }
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoExpressionMethodReference(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoExpressionMethodReference otherExpressionMethodReference) {
            return MoNodeList.sameList(super.typeArguments, otherExpressionMethodReference.typeArguments)
                    && expression.isSame(otherExpressionMethodReference.expression)
                    && simpleName.isSame(otherExpressionMethodReference.simpleName);
        }
        return false;
    }
}
