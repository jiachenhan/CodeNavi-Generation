package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.CastExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.type.MoType;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.Map;


public class MoCastExpression extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoCastExpression.class);
    @Serial
    private static final long serialVersionUID = 1277366874118546360L;

    private final static Description<MoCastExpression, MoType> castTypeDescription =
            new Description<>(ChildType.CHILD, MoCastExpression.class, MoType.class,
                    "type", true);

    private final static Description<MoCastExpression, MoExpression> expressionDescription =
            new Description<>(ChildType.CHILD, MoCastExpression.class, MoExpression.class,
                    "expression", true);

    private final static Map<String, Description<MoCastExpression, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("type", castTypeDescription),
            Map.entry("expression", expressionDescription)
    );


    @RoleDescriptor(type = ChildType.CHILD, role = "type", mandatory = true)
    private MoType castType = null;
    @RoleDescriptor(type = ChildType.CHILD, role = "expression", mandatory = true)
    private MoExpression expression = null;

    public MoCastExpression(String fileName, int startLine, int endLine, CastExpression node) {
        super(fileName, startLine, endLine, node);
        moNodeType = MoNodeType.TYPECastExpression;
    }


    public void setCastType(MoType type) {
        castType = type;
    }

    public void setExpression(MoExpression expression) {
        this.expression = expression;
    }

    public MoType getCastType() {
        return castType;
    }

    public MoExpression getExpression() {
        return expression;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoCastExpression(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoCastExpression, ?> description = descriptionsMap.get(role);
        if(description == castTypeDescription) {
            return castType;
        } else if(description == expressionDescription) {
            return expression;
        } else {
            logger.error("Role {} not found in MoCastExpression", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoCastExpression, ?> description = descriptionsMap.get(role);
        if(description == castTypeDescription) {
            castType = (MoType) value;
        } else if(description == expressionDescription) {
            expression = (MoExpression) value;
        } else {
            logger.error("Role {} not found in MoCastExpression", role);
        }
    }

    public static Map<String, Description<MoCastExpression, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoCastExpression(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoCastExpression moCastExpression) {
            return this.castType.isSame(moCastExpression.castType) &&
                    this.expression.isSame(moCastExpression.expression);
        }
        return false;
    }
}