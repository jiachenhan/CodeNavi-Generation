package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.MethodReference;
import org.eclipse.jdt.core.dom.SuperMethodReference;
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
import java.nio.file.Path;
import java.util.*;

public class MoSuperMethodReference extends MoMethodReference {
    private static final Logger logger = LoggerFactory.getLogger(MoSuperMethodReference.class);
    @Serial
    private static final long serialVersionUID = 609340880842647609L;

    private final static Description<MoSuperMethodReference, MoType> typeArgumentsDescription =
            new Description<>(ChildType.CHILDLIST, MoSuperMethodReference.class, MoType.class,
                    "typeArguments", true);

    private final static Description<MoSuperMethodReference, MoName> qualifierDescription =
            new Description<>(ChildType.CHILD, MoSuperMethodReference.class, MoName.class,
                    "qualifier", false);

    private final static Description<MoSuperMethodReference, MoSimpleName> nameDescription =
            new Description<>(ChildType.CHILD, MoSuperMethodReference.class, MoSimpleName.class,
                    "name", true);

    private final static Map<String, Description<MoSuperMethodReference, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("typeArguments", typeArgumentsDescription),
            Map.entry("qualifier", qualifierDescription),
            Map.entry("name", nameDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "qualifier", mandatory = false)
    private MoName qualifier;
    @RoleDescriptor(type = ChildType.CHILD, role = "name", mandatory = true)
    private MoSimpleName simpleName;

    public MoSuperMethodReference(Path fileName, int startLine, int endLine, SuperMethodReference superMethodReference) {
        super(fileName, startLine, endLine, superMethodReference);
        moNodeType = MoNodeType.TYPEExpressionMethodReference;
        super.typeArguments = new MoNodeList<>(this, typeArgumentsDescription);
    }

    public void setQualifier(MoName qualifier) {
        this.qualifier = qualifier;
    }

    public void setSimpleName(MoSimpleName simpleName) {
        this.simpleName = simpleName;
    }

    public Optional<MoName> getQualifier() {
        return Optional.ofNullable(qualifier);
    }

    public MoSimpleName getSimpleName() {
        return simpleName;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoSuperMethodReference(this);
    }

    @Override
    public List<MoNode> getChildren() {
        List<MoNode> children = new ArrayList<>(typeArguments);
        if(qualifier != null) {
            children.add(qualifier);
        }
        children.add(simpleName);
        return Collections.unmodifiableList(children);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoSuperMethodReference, ?> description = descriptionsMap.get(role);
        if(description == typeArgumentsDescription) {
            return super.typeArguments;
        } else if(description == qualifierDescription) {
            return qualifier;
        } else if(description == nameDescription) {
            return simpleName;
        } else {
            logger.error("Role {} not found in MoSuperMethodReference", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoSuperMethodReference, ?> description = descriptionsMap.get(role);
        if(description == typeArgumentsDescription) {
            super.typeArguments.clear();
            super.typeArguments.addAll((List<MoType>) value);
        } else if(description == qualifierDescription) {
            qualifier = (MoName) value;
        } else if(description == nameDescription) {
            simpleName = (MoSimpleName) value;
        } else {
            logger.error("Role {} not found in MoSuperMethodReference", role);
        }
    }

    public static Map<String, Description<MoSuperMethodReference, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoSuperMethodReference(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoSuperMethodReference otherSuperMethodReference) {
            return MoNodeList.sameList(super.typeArguments, otherSuperMethodReference.typeArguments)
                    && qualifier.isSame(otherSuperMethodReference.qualifier)
                    && simpleName.isSame(otherSuperMethodReference.simpleName);
        }
        return false;
    }
}
