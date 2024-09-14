package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.type.MoType;
import repair.ast.declaration.MoAnonymousClassDeclaration;
import repair.ast.declaration.MoBodyDeclaration;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoClassInstanceCreation extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoClassInstanceCreation.class);
    @Serial
    private static final long serialVersionUID = 8637275512995019352L;

    private final static Description<MoClassInstanceCreation, MoType> typeArgumentsDescription =
            new Description<>(ChildType.CHILDLIST, MoClassInstanceCreation.class, MoType.class,
                    "typeArguments", true);

    private final static Description<MoClassInstanceCreation, MoType> typeDescription =
            new Description<>(ChildType.CHILD, MoClassInstanceCreation.class, MoType.class,
                    "type", true);

    private final static Description<MoClassInstanceCreation, MoExpression> expressionDescription =
            new Description<>(ChildType.CHILD, MoClassInstanceCreation.class, MoExpression.class,
                    "expression", false);

    private final static Description<MoClassInstanceCreation, MoExpression> argumentsDescription =
            new Description<>(ChildType.CHILDLIST, MoClassInstanceCreation.class, MoExpression.class,
                    "arguments", true);

    private final static Description<MoClassInstanceCreation, MoAnonymousClassDeclaration> anonymousDeclDescription =
            new Description<>(ChildType.CHILD, MoClassInstanceCreation.class, MoAnonymousClassDeclaration.class,
                    "anonymousClassDeclaration", false);

    private final static Map<String, Description<MoClassInstanceCreation, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("typeArguments", typeArgumentsDescription),
            Map.entry("type", typeDescription),
            Map.entry("expression", expressionDescription),
            Map.entry("arguments", argumentsDescription),
            Map.entry("anonymousClassDeclaration", anonymousDeclDescription)
    );

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "typeArguments", mandatory = true)
    private final MoNodeList<MoType> typeArguments;

    @RoleDescriptor(type = ChildType.CHILD, role = "type", mandatory = true)
    private MoType type;

    @RoleDescriptor(type = ChildType.CHILD, role = "expression", mandatory = false)
    private MoExpression expression;

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "arguments", mandatory = true)
    private final MoNodeList<MoExpression> arguments;

    @RoleDescriptor(type = ChildType.CHILD, role = "anonymousClassDeclaration", mandatory = false)
    private MoAnonymousClassDeclaration anonymousClassDeclaration;

    public MoClassInstanceCreation(String fileName, int startLine, int endLine, ClassInstanceCreation classInstanceCreation) {
        super(fileName, startLine, endLine, classInstanceCreation);
        moNodeType = MoNodeType.TYPEClassInstanceCreation;
        typeArguments = new MoNodeList<>(this, typeArgumentsDescription);
        arguments = new MoNodeList<>(this, argumentsDescription);
    }

    /**
     * true if the type of the typeArguments are inferred from the expected type, aka diamond operator
     */
    private boolean isTypeInferred;

    public boolean isTypeInferred() {
        return isTypeInferred;
    }

    public void setTypeInferred(boolean typeInferred) {
        isTypeInferred = typeInferred;
    }

    public void addTypeArgument(MoType typeArgument) {
        typeArguments.add(typeArgument);
    }

    public void setType(MoType type) {
        this.type = type;
    }

    public void setExpression(MoExpression expression) {
        this.expression = expression;
    }

    public void addArgument(MoExpression argument) {
        arguments.add(argument);
    }

    public void setAnonymousClassDeclaration(MoAnonymousClassDeclaration anonymousClassDeclaration) {
        this.anonymousClassDeclaration = anonymousClassDeclaration;
    }

    public List<MoType> getTypeArguments() {
        return typeArguments;
    }

    public MoType getType() {
        return type;
    }

    public Optional<MoExpression> getExpression() {
        return Optional.ofNullable(expression);
    }

    public List<MoExpression> getArguments() {
        return arguments;
    }

    public Optional<MoAnonymousClassDeclaration> getAnonymousClassDeclaration() {
        return Optional.ofNullable(anonymousClassDeclaration);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoClassInstanceCreation(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoClassInstanceCreation, ?> description = descriptionsMap.get(role);
        if(description == typeArgumentsDescription) {
            return typeArguments;
        } else if(description == typeDescription) {
            return type;
        } else if(description == expressionDescription) {
            return expression;
        } else if(description == argumentsDescription) {
            return arguments;
        } else if(description == anonymousDeclDescription) {
            return anonymousClassDeclaration;
        } else {
            logger.error("Role {} not found in MoClassInstanceCreation", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoClassInstanceCreation, ?> description = descriptionsMap.get(role);
        if(description == typeArgumentsDescription) {
            typeArguments.clear();
            typeArguments.addAll((List<MoType>) value);
        } else if(description == typeDescription) {
            type = (MoType) value;
        } else if(description == expressionDescription) {
            expression = (MoExpression) value;
        } else if(description == argumentsDescription) {
            arguments.clear();
            arguments.addAll((List<MoExpression>) value);
        } else if(description == anonymousDeclDescription) {
            anonymousClassDeclaration = (MoAnonymousClassDeclaration) value;
        } else {
            logger.error("Role {} not found in MoClassInstanceCreation", role);
        }
    }

    public static Map<String, Description<MoClassInstanceCreation, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoClassInstanceCreation clone = new MoClassInstanceCreation(getFileName(), getStartLine(), getEndLine(), null);
        clone.setTypeInferred(isTypeInferred);
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoClassInstanceCreation classInstanceCreation) {
            boolean match = MoNodeList.sameList(typeArguments, classInstanceCreation.typeArguments);
            match = match && type.isSame(classInstanceCreation.type);
            if(expression == null) {
                match = match && (classInstanceCreation.expression == null);
            } else {
                match = match && expression.isSame(classInstanceCreation.expression);
            }
            match = match && MoNodeList.sameList(arguments, classInstanceCreation.arguments);
            if(anonymousClassDeclaration == null) {
                match = match && (classInstanceCreation.anonymousClassDeclaration == null);
            } else {
                match = match && anonymousClassDeclaration.isSame(classInstanceCreation.anonymousClassDeclaration);
            }
            return match;
        }
        return false;
    }
}
