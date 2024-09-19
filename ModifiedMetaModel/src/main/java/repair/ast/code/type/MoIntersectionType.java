package repair.ast.code.type;

import org.eclipse.jdt.core.dom.IntersectionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MoIntersectionType extends MoType{
    private static final Logger logger = LoggerFactory.getLogger(MoIntersectionType.class);
    @Serial
    private static final long serialVersionUID = -550320123020151425L;

    private final static Description<MoIntersectionType, MoType> typesDescription =
            new Description<>(ChildType.CHILDLIST, MoIntersectionType.class, MoType.class,
                    "types", true);

    private final static Map<String, Description<MoIntersectionType, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("types", typesDescription)
    );

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "types", mandatory = true)
    private final MoNodeList<MoType> types;

    public MoIntersectionType(String fileName, int startLine, int endLine, IntersectionType intersectionType) {
        super(fileName, startLine, endLine, intersectionType);
        moNodeType = MoNodeType.TYPEIntersectionType;
        types = new MoNodeList<>(this, typesDescription);
    }

    public void addType(MoType type) {
        types.add(type);
    }

    public List<MoType> getTypes() {
        return types;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoIntersectionType(this);
    }

    @Override
    public List<MoNode> getChildren() {
        return Collections.unmodifiableList(types);
    }

    @Override
    public boolean isLeaf() {
        return types.isEmpty();
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoIntersectionType, ?> description = descriptionsMap.get(role);
        if(description == typesDescription) {
            return types;
        } else {
            logger.error("Role {} not found in MoIntersectionType", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoIntersectionType, ?> description = descriptionsMap.get(role);
        if(description == typesDescription) {
            types.clear();
            types.addAll((List<MoType>) value);
        } else {
            logger.error("Role {} not found in MoIntersectionType", role);
        }
    }

    public static Map<String, Description<MoIntersectionType, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoIntersectionType(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoIntersectionType otherIntersectionType) {
            return MoNodeList.sameList(types, otherIntersectionType.types);
        }
        return false;
    }
}
