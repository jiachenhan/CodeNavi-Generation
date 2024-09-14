package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.type.MoType;
import repair.ast.code.virtual.MoMethodInvocationArguments;
import repair.ast.code.virtual.MoMethodInvocationTarget;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoMethodInvocation extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoMethodInvocation.class);
    @Serial
    private static final long serialVersionUID = 3760050994706375419L;

    private final static Description<MoMethodInvocation, MoMethodInvocationTarget> expressionDescription =
            new Description<>(ChildType.CHILD, MoMethodInvocation.class, MoMethodInvocationTarget.class,
                    "expression", false);

    private final static Description<MoMethodInvocation, MoType> typeArgumentsDescription =
            new Description<>(ChildType.CHILDLIST, MoMethodInvocation.class, MoType.class,
                    "typeArguments", true);

    private final static Description<MoMethodInvocation, MoSimpleName> nameDescription =
            new Description<>(ChildType.CHILD, MoMethodInvocation.class, MoSimpleName.class,
                    "name", true);

    private final static Description<MoMethodInvocation, MoMethodInvocationArguments> argumentsDescription =
            new Description<>(ChildType.CHILD, MoMethodInvocation.class, MoMethodInvocationArguments.class,
                    "arguments", true);

    private final static Map<String, Description<MoMethodInvocation, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("expression", expressionDescription),
            Map.entry("typeArguments", typeArgumentsDescription),
            Map.entry("name", nameDescription),
            Map.entry("arguments", argumentsDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "expression", mandatory = false)
    private MoMethodInvocationTarget target;
    @RoleDescriptor(type = ChildType.CHILDLIST, role = "typeArguments", mandatory = true)
    private final MoNodeList<MoType> typeArguments;
    @RoleDescriptor(type = ChildType.CHILD, role = "name", mandatory = true)
    private MoSimpleName name;
    @RoleDescriptor(type = ChildType.CHILD, role = "arguments", mandatory = true)
    private MoMethodInvocationArguments arguments;

    public MoMethodInvocation(String fileName, int startLine, int endLine, MethodInvocation methodInvocation) {
        super(fileName, startLine, endLine, methodInvocation);
        moNodeType = MoNodeType.TYPEMethodInvocation;
        typeArguments = new MoNodeList<>(this, typeArgumentsDescription);
    }

    /**
     * true if the type of the typeArguments are inferred from the expected type, aka diamond operator
     */
    private boolean isTypeInferred;

    public boolean isTypeInferred() {
        return isTypeInferred;
    }

    public void setTypeInferred(boolean typeInferred) {
        this.isTypeInferred = typeInferred;
    }

    public void addTypeArgument(MoType typeArgument) {
        typeArguments.add(typeArgument);
    }
    public void setName(MoSimpleName name) {
        this.name = name;
    }
    public Optional<MoMethodInvocationTarget> getTarget() {
        return Optional.ofNullable(target);
    }

    public List<MoType> getTypeArguments() {
        return typeArguments;
    }

    public MoSimpleName getName() {
        return name;
    }

    public MoMethodInvocationArguments getArguments() {
        return arguments;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoMethodInvocation(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoMethodInvocation, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            return target;
        } else if(description == typeArgumentsDescription) {
            return typeArguments;
        } else if(description == nameDescription) {
            return name;
        } else if(description == argumentsDescription) {
            return arguments;
        } else {
            logger.error("Role {} not found in MoMethodInvocation", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoMethodInvocation, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            target = (MoMethodInvocationTarget) value;
        } else if(description == typeArgumentsDescription) {
            typeArguments.clear();
            typeArguments.addAll((List<MoType>) value);
        } else if(description == nameDescription) {
            name = (MoSimpleName) value;
        } else if(description == argumentsDescription) {
            arguments = (MoMethodInvocationArguments) value;
        } else {
            logger.error("Role {} not found in MoMethodInvocation", role);
        }
    }

    public static Map<String, Description<MoMethodInvocation, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoMethodInvocation clone = new MoMethodInvocation(getFileName(), getStartLine(), getEndLine(), null);
        clone.setTypeInferred(isTypeInferred());
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoMethodInvocation otherMethodInvocation) {
            boolean match;
            if(target == null) {
                match = otherMethodInvocation.target == null;
            } else {
                match = target.isSame(otherMethodInvocation.target);
            }
            match = match && MoNodeList.sameList(typeArguments, otherMethodInvocation.typeArguments);
            match = match && name.isSame(otherMethodInvocation.name);
            match = match && arguments.isSame(otherMethodInvocation.arguments);
            return match;
        }
        return false;
    }
}
