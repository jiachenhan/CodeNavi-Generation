package repair.ast.code.type;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.MoDimension;
import repair.ast.declaration.MoAnonymousClassDeclaration;
import repair.ast.declaration.MoBodyDeclaration;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Map;

public class MoArrayType extends MoType {
    private static final Logger logger = LoggerFactory.getLogger(MoArrayType.class);
    @Serial
    private static final long serialVersionUID = -8343158704862609135L;

    private final static Description<MoArrayType, MoType> elementTypeDescription =
            new Description<>(ChildType.CHILD, MoArrayType.class, MoType.class,
                    "elementType", true);

    private final static Description<MoArrayType, MoDimension> dimensionsDescription =
            new Description<>(ChildType.CHILDLIST, MoArrayType.class, MoDimension.class,
                    "dimensions", true);

    private final static Map<String, Description<MoArrayType, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("elementType", elementTypeDescription),
            Map.entry("dimensions", dimensionsDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "elementType", mandatory = true)
    private MoType elementType;
    @RoleDescriptor(type = ChildType.CHILDLIST, role = "dimensions", mandatory = true)
    private final MoNodeList<MoDimension> dimensions;

    public MoArrayType(String fileName, int startLine, int endLine, ArrayType arrayType) {
        super(fileName, startLine, endLine, arrayType);
        moNodeType = MoNodeType.TYPEArrayType;
        dimensions = new MoNodeList<>(this, dimensionsDescription);
    }


    public void setElementType(MoType elementType) {
        this.elementType = elementType;
    }

    public void addDimension(MoDimension dimension) {
        dimensions.add(dimension);
    }

    public MoType getElementType() {
        return elementType;
    }

    public List<MoDimension> getDimensions() {
        return dimensions;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoArrayType(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoArrayType, ?> description = descriptionsMap.get(role);
        if (description == elementTypeDescription) {
            return elementType;
        } else if (description == dimensionsDescription) {
            return dimensions;
        } else {
            logger.error("Role {} not found in MoArrayType", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoArrayType, ?> description = descriptionsMap.get(role);
        if (description == elementTypeDescription) {
            elementType = (MoType) value;
        } else if (description == dimensionsDescription) {
            dimensions.clear();
            dimensions.addAll((List<MoDimension>) value);
        } else {
            logger.error("Role {} not found in MoArrayType", role);
        }
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoArrayType(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if (other instanceof MoArrayType otherArrayType) {
            return elementType.isSame(otherArrayType.elementType) &&
                    MoNodeList.sameList(dimensions, otherArrayType.dimensions);
        }
        return false;
    }
}
