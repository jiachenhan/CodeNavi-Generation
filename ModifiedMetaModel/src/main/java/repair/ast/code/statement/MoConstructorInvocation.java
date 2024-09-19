package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoExpression;
import repair.ast.code.type.MoType;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MoConstructorInvocation extends MoStatement {
    private static final Logger logger = LoggerFactory.getLogger(MoConstructorInvocation.class);
    @Serial
    private static final long serialVersionUID = -3751847055756365027L;

    private final static Description<MoConstructorInvocation, MoType> typeArgumentsDescription =
            new Description<>(ChildType.CHILDLIST, MoConstructorInvocation.class, MoType.class,
                    "typeArguments", true);

    private final static Description<MoConstructorInvocation, MoExpression> argumentsDescription =
            new Description<>(ChildType.CHILDLIST, MoConstructorInvocation.class, MoExpression.class,
                    "arguments", true);

    private final static Map<String, Description<MoConstructorInvocation, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("typeArguments", typeArgumentsDescription),
            Map.entry("arguments", argumentsDescription)
    );

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "typeArguments", mandatory = true)
    private final MoNodeList<MoType> typeArguments;

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "arguments", mandatory = true)
    private final MoNodeList<MoExpression> arguments;

    public MoConstructorInvocation(String fileName, int startLine, int endLine, ConstructorInvocation constructorInvocation) {
        super(fileName, startLine, endLine, constructorInvocation);
        moNodeType = MoNodeType.TYPEConstructorInvocation;
        typeArguments = new MoNodeList<>(this, typeArgumentsDescription);
        arguments = new MoNodeList<>(this, argumentsDescription);
    }

    public void addTypeArgument(MoType typeArguments) {
        this.typeArguments.add(typeArguments);
    }

    public void addArgument(MoExpression arguments) {
        this.arguments.add(arguments);
    }

    public List<MoType> getTypeArguments() {
        return typeArguments;
    }

    public List<MoExpression> getArguments() {
        return arguments;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoConstructorInvocation(this);
    }

    @Override
    public List<MoNode> getChildren() {
        List<MoNode> children = new ArrayList<>();
        children.addAll(typeArguments);
        children.addAll(arguments);
        return Collections.unmodifiableList(children);
    }

    @Override
    public boolean isLeaf() {
        return typeArguments.isEmpty() && arguments.isEmpty();
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoConstructorInvocation, ?> description = descriptionsMap.get(role);
        if(description == typeArgumentsDescription) {
            return typeArguments;
        } else if(description == argumentsDescription) {
            return arguments;
        } else {
            logger.error("Role {} not found in MoConstructorInvocation", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoConstructorInvocation, ?> description = descriptionsMap.get(role);
        if(description == typeArgumentsDescription) {
            typeArguments.clear();
            typeArguments.addAll((List<MoType>) value);
        } else if(description == argumentsDescription) {
            arguments.clear();
            arguments.addAll((List<MoExpression>) value);
        } else {
            logger.error("Role {} not found in MoConstructorInvocation", role);
            return;
        }
    }

    public static Map<String, Description<MoConstructorInvocation, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoConstructorInvocation(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoConstructorInvocation constructorInvocation){
            return MoNodeList.sameList(typeArguments, constructorInvocation.typeArguments) &&
                    MoNodeList.sameList(arguments, constructorInvocation.arguments);
        }
        return false;
    }
}
