package repair.ast.code.type;

import org.eclipse.jdt.core.dom.AnnotatableType;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoAnnotation;
import repair.ast.code.expression.MoSimpleName;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * QualifiedType:
 *      Type . { Annotation } SimpleName
 */
public class MoQualifiedType extends MoAnnotatableType {
    private static final Logger logger = LoggerFactory.getLogger(MoQualifiedType.class);
    @Serial
    private static final long serialVersionUID = 4854812331382817678L;

    private final static Description<MoQualifiedType, MoAnnotation> annotationsDescription =
            new Description<>(ChildType.CHILDLIST, MoQualifiedType.class, MoAnnotation.class,
                    "annotations", true);

    private final static Description<MoQualifiedType, MoType> qualifierDescription =
            new Description<>(ChildType.CHILD, MoQualifiedType.class, MoType.class,
                    "qualifier", true);

    private final static Description<MoQualifiedType, MoSimpleName> nameDescription =
            new Description<>(ChildType.CHILD, MoQualifiedType.class, MoSimpleName.class,
                    "name", true);

    private final static Map<String, Description<MoQualifiedType, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("annotations", annotationsDescription),
            Map.entry("qualifier", qualifierDescription),
            Map.entry("name", nameDescription)
    );
    @RoleDescriptor(type = ChildType.CHILD, role = "qualifier", mandatory = true)
    private MoType qualifier;
    @RoleDescriptor(type = ChildType.CHILD, role = "name", mandatory = true)
    private MoSimpleName simpleName;

    public MoQualifiedType(String fileName, int startLine, int endLine, QualifiedType qualifiedType) {
        super(fileName, startLine, endLine, qualifiedType);
        moNodeType = MoNodeType.TYPEQualifiedType;
        super.annotations = new MoNodeList<>(this, annotationsDescription);

    }

    public void setQualifier(MoType qualifier) {
        this.qualifier = qualifier;
    }

    public void setSimpleName(MoSimpleName simpleName) {
        this.simpleName = simpleName;
    }

    public MoType getQualifier() {
        return qualifier;
    }

    public MoSimpleName getSimpleName() {
        return simpleName;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoQualifiedType(this);
    }

    @Override
    public List<MoNode> getChildren() {
        List<MoNode> children = new ArrayList<>(annotations);
        children.add(qualifier);
        children.add(simpleName);
        return Collections.unmodifiableList(children);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoQualifiedType, ?> description = descriptionsMap.get(role);
        if(description == annotationsDescription) {
            return super.annotations;
        } else if(description == qualifierDescription) {
            return qualifier;
        } else if(description == nameDescription) {
            return simpleName;
        } else {
            logger.error("Role {} not found in MoQualifiedType", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoQualifiedType, ?> description = descriptionsMap.get(role);
        if(description == annotationsDescription) {
            super.annotations.clear();
            super.annotations.addAll((MoNodeList<MoAnnotation>) value);
        } else if(description == qualifierDescription) {
            this.qualifier = (MoType) value;
        } else if(description == nameDescription) {
            this.simpleName = (MoSimpleName) value;
        } else {
            logger.error("Role {} not found in MoQualifiedType", role);
        }
    }

    public static Map<String, Description<MoQualifiedType, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoQualifiedType(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoQualifiedType moQualifiedType) {
            return MoNodeList.sameList(annotations, moQualifiedType.annotations) &&
                    qualifier.isSame(moQualifiedType.qualifier) &&
                    simpleName.isSame(moQualifiedType.simpleName);
        }
        return false;
    }
}
