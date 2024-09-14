package repair.ast.code.virtual;

import org.eclipse.jdt.core.dom.ASTNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoExpression;
import repair.ast.code.expression.MoMethodInvocation;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Map;

public class MoMethodInvocationArguments extends MoVirtualChildListNode {
    private static final Logger logger = LoggerFactory.getLogger(MoMethodInvocationTarget.class);
    @Serial
    private static final long serialVersionUID = 3771451292521550628L;

    private final static Description<MoMethodInvocationArguments, MoExpression> argumentsDescription =
            new Description<>(ChildType.CHILDLIST, MoMethodInvocationArguments.class, MoExpression.class,
                    "arguments", true);

    private final static Map<String, Description<MoMethodInvocationArguments, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("arguments", argumentsDescription)
    );

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "arguments", mandatory = true)
    private final MoNodeList<MoExpression> arguments;

    public MoMethodInvocationArguments(String fileName, int startLine, int endLine, ASTNode oriNode) {
        super(fileName, startLine, endLine, oriNode);
        moNodeType = MoNodeType.TYPEMethodInvocationArguments;
        arguments = new MoNodeList<>(this, argumentsDescription);
    }

    public MoNodeList<MoExpression> getArguments() {
        return arguments;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoMethodInvocationArguments, ?> description = descriptionsMap.get(role);
        if(description == argumentsDescription) {
            return arguments;
        } else {
            logger.error("Role {} not found in MoMethodInvocationArguments", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoMethodInvocationArguments, ?> description = descriptionsMap.get(role);
        if(description == argumentsDescription) {
            arguments.clear();
            arguments.addAll(((List<MoExpression>) value));
        } else {
            logger.error("Role {} not found in MoMethodInvocationArguments", role);
        }
    }

    public static Map<String, Description<MoMethodInvocationArguments, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoMethodInvocationArguments(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoMethodInvocationArguments otherMethodInvocationArguments) {
            return MoNodeList.sameList(arguments, otherMethodInvocationArguments.arguments);
        }
        return false;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoMethodInvocationArguments(this);
    }
}
