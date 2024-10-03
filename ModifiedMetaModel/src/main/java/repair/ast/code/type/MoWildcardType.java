package repair.ast.code.type;

import org.eclipse.jdt.core.dom.AnnotatableType;
import org.eclipse.jdt.core.dom.WildcardType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.MoExtendedModifier;
import repair.ast.code.expression.MoAnnotation;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.nio.file.Path;
import java.util.*;

public class MoWildcardType extends MoAnnotatableType {
    private static final Logger logger = LoggerFactory.getLogger(MoWildcardType.class);
    @Serial
    private static final long serialVersionUID = 8285183722272787325L;

    private final static Description<MoWildcardType, MoAnnotation> annotationsDescription =
            new Description<>(ChildType.CHILDLIST, MoWildcardType.class, MoAnnotation.class,
                    "annotations", true);

    private final static Description<MoWildcardType, MoType> boundDescription =
            new Description<>(ChildType.CHILD, MoWildcardType.class, MoType.class,
                    "bound", false);

    private final static Description<MoWildcardType, Boolean> upperBoundDescription =
            new Description<>(ChildType.SIMPLE, MoWildcardType.class, Boolean.class,
                    "upperBound", true);

    private final static Map<String, Description<MoWildcardType, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("annotations", annotationsDescription),
            Map.entry("bound", boundDescription),
            Map.entry("upperBound", upperBoundDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "bound", mandatory = false)
    private MoType bound;
    @RoleDescriptor(type = ChildType.SIMPLE, role = "upperBound", mandatory = true)
    private boolean upperBound;

    public MoWildcardType(Path fileName, int startLine, int endLine, WildcardType wildcardType) {
        super(fileName, startLine, endLine, wildcardType);
        moNodeType = MoNodeType.TYPEWildcardType;
        super.annotations = new MoNodeList<>(this, annotationsDescription);
    }

    public void setBound(MoType bound) {
        this.bound = bound;
    }

    public Optional<MoType> getBound() {
        return Optional.ofNullable(bound);
    }

    public boolean isUpperBound() {
        return upperBound;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoWildcardType(this);
    }

    @Override
    public List<MoNode> getChildren() {
        List<MoNode> children = new ArrayList<>(annotations);
        if(bound != null) {
            children.add(bound);
        }
        return Collections.unmodifiableList(children);
    }

    @Override
    public boolean isLeaf() {
        return annotations.isEmpty() && bound == null;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoWildcardType, ?> description = descriptionsMap.get(role);
        if(description == annotationsDescription) {
            return super.annotations;
        } else if(description == boundDescription) {
            return bound;
        } else if(description == upperBoundDescription) {
            return upperBound;
        } else {
            logger.error("Role {} not found in MoWildcardType", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoWildcardType, ?> description = descriptionsMap.get(role);
        if(description == annotationsDescription) {
            super.annotations.clear();
            super.annotations.addAll((MoNodeList<MoAnnotation>) value);
        } else if(description == boundDescription) {
            bound = (MoType) value;
        } else if(description == upperBoundDescription) {
            upperBound = (boolean) value;
        } else {
            logger.error("Role {} not found in MoWildcardType", role);
        }
    }

    public static Map<String, Description<MoWildcardType, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoWildcardType clone = new MoWildcardType(getFileName(), getStartLine(), getEndLine(), null);
        clone.setStructuralProperty("upperBound", isUpperBound());
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoWildcardType wildcardType) {
            boolean match = MoExtendedModifier.sameList(annotations, wildcardType.annotations);
            if(bound == null) {
                return match && wildcardType.bound == null;
            } else {
                match = match && bound.isSame(wildcardType.bound);
                return match && upperBound == wildcardType.upperBound;
            }
        }
        return false;
    }
}
