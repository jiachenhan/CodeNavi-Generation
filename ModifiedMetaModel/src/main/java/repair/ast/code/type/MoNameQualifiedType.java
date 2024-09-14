package repair.ast.code.type;

import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.MoExtendedModifier;
import repair.ast.code.expression.MoAnnotation;
import repair.ast.code.expression.MoName;
import repair.ast.code.expression.MoSimpleName;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.Map;

public class MoNameQualifiedType extends MoAnnotatableType {
    private static final Logger logger = LoggerFactory.getLogger(MoPrimitiveType.class);
    @Serial
    private static final long serialVersionUID = 5960991149630266710L;

    private final static Description<MoNameQualifiedType, MoAnnotation> annotationsDescription =
            new Description<>(ChildType.CHILDLIST, MoNameQualifiedType.class, MoAnnotation.class,
                    "annotations", true);

    private final static Description<MoNameQualifiedType, MoName> qualifierDescription =
            new Description<>(ChildType.CHILD, MoNameQualifiedType.class, MoName.class,
                    "qualifier", true);

    private final static Description<MoNameQualifiedType, MoSimpleName> nameDescription =
            new Description<>(ChildType.CHILD, MoNameQualifiedType.class, MoSimpleName.class,
                    "name", true);

    private final static Map<String, Description<MoNameQualifiedType, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("annotations", annotationsDescription),
            Map.entry("qualifier", qualifierDescription),
            Map.entry("name", nameDescription)
    );


    @RoleDescriptor(type = ChildType.CHILD, role = "qualifier", mandatory = true)
    private MoName qualifier;
    @RoleDescriptor(type = ChildType.CHILD, role = "name", mandatory = true)
    private MoSimpleName simpleName;

    public MoNameQualifiedType(String fileName, int startLine, int endLine, NameQualifiedType nameQualifiedType) {
        super(fileName, startLine, endLine, nameQualifiedType);
        moNodeType = MoNodeType.TYPENameQualifiedType;
        super.annotations = new MoNodeList<>(this, annotationsDescription);
    }

    public void setQualifier(MoName qualifier) {
        this.qualifier = qualifier;
    }

    public void setSimpleName(MoSimpleName simpleName) {
        this.simpleName = simpleName;
    }

    public MoName getQualifier() {
        return qualifier;
    }

    public MoSimpleName getSimpleName() {
        return simpleName;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoNameQualifiedType(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoNameQualifiedType, ?> description = descriptionsMap.get(role);
        if(description == annotationsDescription) {
            return super.annotations;
        } else if(description == qualifierDescription) {
            return qualifier;
        } else if(description == nameDescription) {
            return simpleName;
        } else {
            logger.error("Role {} not found in MoNameQualifiedType", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoNameQualifiedType, ?> description = descriptionsMap.get(role);
        if(description == annotationsDescription) {
            super.annotations.clear();
            super.annotations.addAll((MoNodeList<MoAnnotation>) value);
        } else if(description == qualifierDescription) {
            qualifier = (MoName) value;
        } else if(description == nameDescription) {
            simpleName = (MoSimpleName) value;
        } else {
            logger.error("Role {} not found in MoNameQualifiedType", role);
        }
    }

    public static Map<String, Description<MoNameQualifiedType, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoNameQualifiedType(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoNameQualifiedType otherNameQualifiedType) {
            return MoExtendedModifier.sameList(annotations, otherNameQualifiedType.annotations) &&
                    qualifier.isSame(otherNameQualifiedType.qualifier) &&
                    simpleName.isSame(otherNameQualifiedType.simpleName);
        }
        return false;
    }
}
