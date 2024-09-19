package repair.ast.code.expression.literal;

import org.eclipse.jdt.core.dom.TypeLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoExpression;
import repair.ast.code.type.MoType;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Map;

public class MoTypeLiteral extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoTypeLiteral.class);
    @Serial
    private static final long serialVersionUID = -16805188302075940L;
    private static final String postfix = ".class";
    private final static Description<MoTypeLiteral, MoType> typeDescription =
            new Description<>(ChildType.CHILD, MoTypeLiteral.class, MoType.class,
                    "type", true);

    private final static Map<String, Description<MoTypeLiteral, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("type", typeDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "type", mandatory = true)
    private MoType type;

    public MoTypeLiteral(String fileName, int startLine, int endLine, TypeLiteral typeLiteral) {
        super(fileName, startLine, endLine, typeLiteral);
        moNodeType = MoNodeType.TYPETypeLiteral;
    }


    public void setType(MoType type) {
        this.type = type;
    }

    public MoType getType() {
        return type;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoTypeLiteral(this);
    }

    @Override
    public List<MoNode> getChildren() {
        return List.of(type);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoTypeLiteral, ?> description = descriptionsMap.get(role);
        if(description == typeDescription) {
            return type;
        } else {
            logger.error("Role {} not found in MoTypeLiteral", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoTypeLiteral, ?> description = descriptionsMap.get(role);
        if(description == typeDescription) {
            type = (MoType) value;
        } else {
            logger.error("Role {} not found in MoTypeLiteral", role);
        }
    }

    public static Map<String, Description<MoTypeLiteral, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoTypeLiteral(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoTypeLiteral otherTypeLiteral) {
            return type.isSame(otherTypeLiteral.type);
        }
        return false;
    }
}
