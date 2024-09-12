package repair.ast.code.type;

import org.eclipse.jdt.core.dom.ParameterizedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.statement.MoVariableDeclarationStatement;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MoParameterizedType extends MoType {
    private static final Logger logger = LoggerFactory.getLogger(MoParameterizedType.class);
    @Serial
    private static final long serialVersionUID = -248401576967061481L;

    private final static Description<MoParameterizedType, MoType> typeDescription =
            new Description<>(ChildType.CHILD, MoParameterizedType.class, MoType.class,
                    "type", true);

    private final static Description<MoParameterizedType, MoType> typeArgumentsDescription =
            new Description<>(ChildType.CHILDLIST, MoParameterizedType.class, MoType.class,
                    "typeArguments", true);

    private final static Map<String, Description<MoParameterizedType, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("type", typeDescription),
            Map.entry("typeArguments", typeArgumentsDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "type", mandatory = true)
    private MoType type;
    @RoleDescriptor(type = ChildType.CHILDLIST, role = "typeArguments", mandatory = true)
    private final MoNodeList<MoType> typeArguments;

    public MoParameterizedType(String fileName, int startLine, int endLine, ParameterizedType parameterizedType) {
        super(fileName, startLine, endLine, parameterizedType);
        moNodeType = MoNodeType.TYPEParameterizedType;
        typeArguments = new MoNodeList<>(this, typeArgumentsDescription);
    }

    public void setType(MoType type) {
        this.type = type;
    }

    public void addTypeArgument(MoType type) {
        typeArguments.add(type);
    }

    public MoType getType() {
        return type;
    }

    public List<MoType> getTypeArguments() {
        return typeArguments;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoParameterizedType(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoParameterizedType, ?> description = descriptionsMap.get(role);
        if(description == typeDescription) {
            return type;
        } else if(description == typeArgumentsDescription) {
            return typeArguments;
        } else {
            logger.error("Role {} not found in MoParameterizedType", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoParameterizedType, ?> description = descriptionsMap.get(role);
        if(description == typeDescription) {
            this.type = (MoType) value;
        } else if(description == typeArgumentsDescription) {
            typeArguments.clear();
            typeArguments.addAll((List<MoType>) value);
        } else {
            logger.error("Role {} not found in MoParameterizedType", role);
        }
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoParameterizedType(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoParameterizedType moParameterizedType) {
            boolean match = type.isSame(moParameterizedType.type);
            match = match && MoNodeList.sameList(typeArguments, moParameterizedType.typeArguments);
            return match;
        }
        return false;
    }
}
