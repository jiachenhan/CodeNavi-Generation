package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.MethodReference;
import org.eclipse.jdt.core.dom.TypeMethodReference;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MoTypeMethodReference extends MoMethodReference {
    private static final Logger logger = LoggerFactory.getLogger(MoTypeMethodReference.class);
    @Serial
    private static final long serialVersionUID = 67962754403441491L;

    private final static Description<MoTypeMethodReference, MoType> typeArgumentsDescription =
            new Description<>(ChildType.CHILDLIST, MoTypeMethodReference.class, MoType.class,
                    "typeArguments", true);

    private final static Description<MoTypeMethodReference, MoType> typeDescription =
            new Description<>(ChildType.CHILD, MoTypeMethodReference.class, MoType.class,
                    "type", true);

    private final static Description<MoTypeMethodReference, MoSimpleName> nameDescription =
            new Description<>(ChildType.CHILD, MoTypeMethodReference.class, MoSimpleName.class,
                    "name", true);

    private final static Map<String, Description<MoTypeMethodReference, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("typeArguments", typeArgumentsDescription),
            Map.entry("type", typeDescription),
            Map.entry("name", nameDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "type", mandatory = true)
    private MoType type;
    @RoleDescriptor(type = ChildType.CHILD, role = "name", mandatory = true)
    private MoSimpleName simpleName;

    public MoTypeMethodReference(String fileName, int startLine, int endLine, TypeMethodReference typeMethodReference) {
        super(fileName, startLine, endLine, typeMethodReference);
        moNodeType = MoNodeType.TYPETypeMethodReference;
    }

    public void setType(MoType type) {
        this.type = type;
    }

    public void setSimpleName(MoSimpleName simpleName) {
        this.simpleName = simpleName;
    }

    public MoType getType() {
        return type;
    }

    public MoSimpleName getSimpleName() {
        return simpleName;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoTypeMethodReference(this);
    }

    @Override
    public List<MoNode> getChildren() {
        List<MoNode> children = new ArrayList<>(typeArguments);
        children.add(type);
        children.add(simpleName);
        return Collections.unmodifiableList(children);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoTypeMethodReference, ?> description = descriptionsMap.get(role);
        if(description == typeArgumentsDescription) {
            return super.typeArguments;
        } else if(description == typeDescription) {
            return type;
        } else if(description == nameDescription) {
            return simpleName;
        } else {
            logger.error("Role {} not found in MoTypeMethodReference", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoTypeMethodReference, ?> description = descriptionsMap.get(role);
        if(description == typeArgumentsDescription) {
            super.typeArguments.clear();
            super.typeArguments.addAll((List<MoType>) value);
        } else if(description == typeDescription) {
            type = (MoType) value;
        } else if(description == nameDescription) {
            simpleName = (MoSimpleName) value;
        } else {
            logger.error("Role {} not found in MoTypeMethodReference", role);
        }
    }

    public static Map<String, Description<MoTypeMethodReference, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoTypeMethodReference(getFileName(), getStartLine(), getEndLine(), null);
    }


    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoTypeMethodReference otherTypeMethodReference) {
            return MoNodeList.sameList(super.typeArguments, otherTypeMethodReference.typeArguments)
                    && type.isSame(otherTypeMethodReference.type)
                    && simpleName.isSame(otherTypeMethodReference.simpleName);
        }
        return false;
    }
}
