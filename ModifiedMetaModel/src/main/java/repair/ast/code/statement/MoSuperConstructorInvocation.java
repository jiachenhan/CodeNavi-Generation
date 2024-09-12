package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.MoExtendedModifier;
import repair.ast.code.expression.MoExpression;
import repair.ast.code.type.MoType;
import repair.ast.declaration.MoFieldDeclaration;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoSuperConstructorInvocation extends MoStatement {
    private static final Logger logger = LoggerFactory.getLogger(MoSuperConstructorInvocation.class);
    @Serial
    private static final long serialVersionUID = -3508770477241694623L;

    private final static Description<MoSuperConstructorInvocation, MoExpression> expressionDescription =
            new Description<>(ChildType.CHILD, MoSuperConstructorInvocation.class, MoExpression.class,
                    "expression", false);

    private final static Description<MoSuperConstructorInvocation, MoType> typeArgumentsDescription =
            new Description<>(ChildType.CHILDLIST, MoSuperConstructorInvocation.class, MoType.class,
                    "typeArguments", true);

    private final static Description<MoSuperConstructorInvocation, MoExpression> argumentsDescription =
            new Description<>(ChildType.CHILDLIST, MoSuperConstructorInvocation.class, MoExpression.class,
                    "arguments", true);

    private final static Map<String, Description<MoSuperConstructorInvocation, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("expression", expressionDescription),
            Map.entry("typeArguments", typeArgumentsDescription),
            Map.entry("arguments", argumentsDescription)
    );


    @RoleDescriptor(type = ChildType.CHILD, role = "expression", mandatory = false)
    private MoExpression expression;
    @RoleDescriptor(type = ChildType.CHILDLIST, role = "typeArguments", mandatory = true)
    private final MoNodeList<MoType> typeArguments;
    @RoleDescriptor(type = ChildType.CHILDLIST, role = "arguments", mandatory = true)
    private final MoNodeList<MoExpression> arguments;

    public MoSuperConstructorInvocation(String fileName, int startLine, int endLine, SuperConstructorInvocation superConstructorInvocation) {
        super(fileName, startLine, endLine, superConstructorInvocation);
        moNodeType = MoNodeType.TYPESuperConstructorInvocation;
        typeArguments = new MoNodeList<>(this, typeArgumentsDescription);
        arguments = new MoNodeList<>(this, argumentsDescription);
    }

    public void setExpression(MoExpression expression) {
        this.expression = expression;
    }

    public void addTypeArgument(MoType type) {
        typeArguments.add(type);
    }

    public void addArgument(MoExpression argument) {
        arguments.add(argument);
    }

    public Optional<MoExpression> getExpression() {
        return Optional.ofNullable(expression);
    }

    public List<MoType> getTypeArguments() {
        return typeArguments;
    }

    public List<MoExpression> getArguments() {
        return arguments;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoSuperConstructorInvocation(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoSuperConstructorInvocation, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            return expression;
        } else if(description == typeArgumentsDescription) {
            return typeArguments;
        } else if(description == argumentsDescription) {
            return arguments;
        } else {
            logger.error("Role {} not found in MoSuperConstructorInvocation", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoSuperConstructorInvocation, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            expression = (MoExpression) value;
        } else if(description == typeArgumentsDescription) {
            typeArguments.clear();
            typeArguments.addAll((List<MoType>) value);
        } else if(description == argumentsDescription) {
            arguments.clear();
            arguments.addAll((List<MoExpression>) value);
        } else {
            logger.error("Role {} not found in MoSuperConstructorInvocation", role);
        }
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoSuperConstructorInvocation(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoSuperConstructorInvocation moSuperConstructorInvocation) {
            boolean match;
            if(this.expression == null) {
                match = moSuperConstructorInvocation.expression == null;
            } else {
                match = this.expression.isSame(moSuperConstructorInvocation.expression);
            }
            match = match && MoNodeList.sameList(this.typeArguments, moSuperConstructorInvocation.typeArguments);
            match = match && MoNodeList.sameList(this.arguments, moSuperConstructorInvocation.arguments);
            return match;
        }
        return false;
    }
}
