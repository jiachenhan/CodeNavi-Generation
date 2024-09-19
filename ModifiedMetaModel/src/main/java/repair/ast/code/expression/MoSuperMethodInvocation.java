package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.MoExtendedModifier;
import repair.ast.code.MoJavadoc;
import repair.ast.code.type.MoType;
import repair.ast.declaration.MoFieldDeclaration;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.*;

public class MoSuperMethodInvocation extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoSuperMethodInvocation.class);
    @Serial
    private static final long serialVersionUID = 1270019378691696758L;

    private final static Description<MoSuperMethodInvocation, MoName> qualifierDescription =
            new Description<>(ChildType.CHILD, MoSuperMethodInvocation.class, MoName.class,
                    "qualifier", false);

    private final static Description<MoSuperMethodInvocation, MoType> typeArgumentsDescription =
            new Description<>(ChildType.CHILDLIST, MoSuperMethodInvocation.class, MoType.class,
                    "typeArguments", true);

    private final static Description<MoSuperMethodInvocation, MoSimpleName> nameDescription =
            new Description<>(ChildType.CHILD, MoSuperMethodInvocation.class, MoSimpleName.class,
                    "name", true);

    private final static Description<MoSuperMethodInvocation, MoExpression> argumentsDescription =
            new Description<>(ChildType.CHILDLIST, MoSuperMethodInvocation.class, MoExpression.class,
                    "arguments", true);

    private final static Map<String, Description<MoSuperMethodInvocation, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("qualifier", qualifierDescription),
            Map.entry("typeArguments", typeArgumentsDescription),
            Map.entry("name", nameDescription),
            Map.entry("arguments", argumentsDescription)
    );


    @RoleDescriptor(type = ChildType.CHILD, role = "qualifier", mandatory = false)
    private MoName qualifier;
    @RoleDescriptor(type = ChildType.CHILDLIST, role = "typeArguments", mandatory = true)
    private final MoNodeList<MoType> typeArguments;
    @RoleDescriptor(type = ChildType.CHILD, role = "name", mandatory = true)
    private MoSimpleName name;
    @RoleDescriptor(type = ChildType.CHILDLIST, role = "arguments", mandatory = true)
    private final MoNodeList<MoExpression> arguments;

    public MoSuperMethodInvocation(String fileName, int startLine, int endLine, SuperMethodInvocation superMethodInvocation) {
        super(fileName, startLine, endLine, superMethodInvocation);
        moNodeType = MoNodeType.TYPESuperMethodInvocation;
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

    public void setQualifier(MoName qualifier) {
        this.qualifier = qualifier;
    }

    public void addTypeArgument(MoType typeArgument) {
        typeArguments.add(typeArgument);
    }

    public void setName(MoSimpleName name) {
        this.name = name;
    }

    public void addArgument(MoExpression argument) {
        arguments.add(argument);
    }

    public Optional<MoName> getQualifier() {
        return Optional.ofNullable(qualifier);
    }

    public List<MoType> getTypeArguments() {
        return typeArguments;
    }

    public MoSimpleName getName() {
        return name;
    }

    public List<MoExpression> getArguments() {
        return arguments;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoSuperMethodInvocation(this);
    }

    @Override
    public List<MoNode> getChildren() {
        List<MoNode> children = new ArrayList<>();
        if(qualifier != null) {
            children.add(qualifier);
        }
        children.addAll(typeArguments);
        children.add(name);
        children.addAll(arguments);
        return Collections.unmodifiableList(children);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoSuperMethodInvocation, ?> description = descriptionsMap.get(role);
        if(description == qualifierDescription) {
            return qualifier;
        } else if(description == typeArgumentsDescription) {
            return typeArguments;
        } else if(description == nameDescription) {
            return name;
        } else if(description == argumentsDescription) {
            return arguments;
        } else {
            logger.error("Role {} not found in MoSuperMethodInvocation", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoSuperMethodInvocation, ?> description = descriptionsMap.get(role);
        if(description == qualifierDescription) {
            this.qualifier = (MoName) value;
        } else if(description == typeArgumentsDescription) {
            typeArguments.clear();
            typeArguments.addAll((List<MoType>) value);
        } else if(description == nameDescription) {
            this.name = (MoSimpleName) value;
        } else if(description == argumentsDescription) {
            arguments.clear();
            arguments.addAll((List<MoExpression>) value);
        } else {
            logger.error("Role {} not found in MoSuperMethodInvocation", role);
        }
    }

    public static Map<String, Description<MoSuperMethodInvocation, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoSuperMethodInvocation clone = new MoSuperMethodInvocation(getFileName(), getStartLine(), getEndLine(), null);
        clone.setTypeInferred(isTypeInferred());
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoSuperMethodInvocation moSuperMethodInvocation) {
            boolean match = name.isSame(moSuperMethodInvocation.name);
            if(qualifier == null) {
                match = match && moSuperMethodInvocation.qualifier == null;
            } else {
                match = match && qualifier.isSame(moSuperMethodInvocation.qualifier);
            }
            match = match && MoNodeList.sameList(typeArguments, moSuperMethodInvocation.typeArguments);
            match = match && MoNodeList.sameList(arguments, moSuperMethodInvocation.arguments);
            return match;
        }
        return false;
    }
}
