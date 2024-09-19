package repair.ast.code.expression.literal;

import org.eclipse.jdt.core.dom.NullLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoExpression;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Map;

public class MoNullLiteral extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoNullLiteral.class);
    @Serial
    private static final long serialVersionUID = -7292223421508744998L;

    private final static Description<MoNullLiteral, String> nullDescription =
            new Description<>(ChildType.SIMPLE, MoNullLiteral.class, String.class,
                    "value", true);

    private final static Map<String, Description<MoNullLiteral, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("value", nullDescription)
    );

    @RoleDescriptor(type = ChildType.SIMPLE, role = "value", mandatory = true)
    private final String nullValue = "null";

    public MoNullLiteral(String fileName, int startLine, int endLine, NullLiteral nullLiteral) {
        super(fileName, startLine, endLine, nullLiteral);
        moNodeType = MoNodeType.TYPENullLiteral;
    }

    public String getNullValue() {
        return nullValue;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoNullLiteral(this);
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
        Description<MoNullLiteral, ?> description = descriptionsMap.get(role);
        if(description == nullDescription) {
            return nullValue;
        } else {
            logger.error("Role {} not found in MoNullLiteral", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoNullLiteral, ?> description = descriptionsMap.get(role);
        if(description == nullDescription) {
            logger.error("Cannot set value for null literal");
        } else {
            logger.error("Role {} not found in MoNullLiteral", role);
        }
    }

    public static Map<String, Description<MoNullLiteral, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoNullLiteral(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        return other instanceof MoNullLiteral;
    }
}
