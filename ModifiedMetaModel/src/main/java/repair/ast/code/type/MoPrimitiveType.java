package repair.ast.code.type;

import org.eclipse.jdt.core.dom.PrimitiveType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoAnnotation;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Map;

public class MoPrimitiveType extends MoAnnotatableType {
    private static final Logger logger = LoggerFactory.getLogger(MoPrimitiveType.class);
    @Serial
    private static final long serialVersionUID = 769946862030219218L;

    private final static Description<MoPrimitiveType, MoAnnotation> annotationsDescription =
            new Description<>(ChildType.CHILDLIST, MoPrimitiveType.class, MoAnnotation.class,
                    "annotations", true);

    private final static Description<MoPrimitiveType, MoPrimitiveType.TypeKind> primitiveTypeCodeDescription =
            new Description<>(ChildType.CHILD, MoPrimitiveType.class, MoPrimitiveType.TypeKind.class,
                    "primitiveTypeCode", true);

    private final static Map<String, Description<MoPrimitiveType, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("annotations", annotationsDescription),
            Map.entry("primitiveTypeCode", primitiveTypeCodeDescription)
    );

    @RoleDescriptor(type = ChildType.SIMPLE, role = "primitiveTypeCode", mandatory = true)
    private MoPrimitiveType.TypeKind typeKind;

    @Override
    public boolean isSame(MoNode other) {
        if (other instanceof MoPrimitiveType moPrimitiveType) {
            return MoNodeList.sameList(this.annotations, moPrimitiveType.annotations) &&
                    moPrimitiveType.typeKind.equals(this.typeKind);
        }
        return false;
    }

    public enum TypeKind{
        BYTE("byte"),
        SHORT("short"),
        CHAR("char"),
        INT("int"),
        LONG("long"),
        FLOAT("float"),
        DOUBLE("double"),
        BOOLEAN("boolean"),
        VOID("void");

        private final String keyword;
        TypeKind(String keyword){
            this.keyword = keyword;
        }

        public static TypeKind fromCode(String value) {
            for (TypeKind typeKind : TypeKind.values()) {
                if (typeKind.keyword.equals(value)) {
                    return typeKind;
                }
            }
            throw new IllegalArgumentException("No enum constant for operator: " + value);
        }

        @Override
        public String toString(){
            return keyword;
        }
    }

    public MoPrimitiveType(String fileName, int startLine, int endLine, PrimitiveType primitiveType) {
        super(fileName, startLine, endLine, primitiveType);
        moNodeType = MoNodeType.TYPEPrimitiveType;
        super.annotations = new MoNodeList<>(this, annotationsDescription);
    }

    public MoPrimitiveType.TypeKind getTypeKind() {
        return typeKind;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoPrimitiveType(this);
    }

    @Override
    public List<MoNode> getChildren() {
        return List.of();
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoPrimitiveType, ?> description = descriptionsMap.get(role);
        if(description == annotationsDescription) {
            return super.annotations;
        } else if(description == primitiveTypeCodeDescription) {
            return typeKind;
        } else {
            logger.error("Role {} not found in MoPrimitiveType", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoPrimitiveType, ?> description = descriptionsMap.get(role);
        if(description == annotationsDescription) {
            super.annotations.clear();
            super.annotations.addAll((MoNodeList<MoAnnotation>) value);
        } else if(description == primitiveTypeCodeDescription) {
            this.typeKind =  TypeKind.fromCode((String) value);
        } else {
            logger.error("Role {} not found in MoPrimitiveType", role);
        }
    }

    public static Map<String, Description<MoPrimitiveType, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoPrimitiveType clone = new MoPrimitiveType(getFileName(), getStartLine(), getEndLine(), null);
        clone.setStructuralProperty("primitiveTypeCode", getTypeKind().toString());
        return clone;
    }

}
