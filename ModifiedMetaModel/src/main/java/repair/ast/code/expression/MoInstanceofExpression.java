package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.MoExtendedModifier;
import repair.ast.code.statement.MoVariableDeclarationStatement;
import repair.ast.code.type.MoType;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Map;

public class MoInstanceofExpression extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoInstanceofExpression.class);
    @Serial
    private static final long serialVersionUID = -2747480747639421795L;

    private final static Description<MoInstanceofExpression, MoExpression> leftOperandDescription =
            new Description<>(ChildType.CHILD, MoInstanceofExpression.class, MoExpression.class,
                    "leftOperand", true);

    private final static Description<MoInstanceofExpression, MoType> rightOperandDescription =
            new Description<>(ChildType.CHILD, MoInstanceofExpression.class, MoType.class,
                    "rightOperand", true);

    private final static Map<String, Description<MoInstanceofExpression, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("leftOperand", leftOperandDescription),
            Map.entry("rightOperand", rightOperandDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "leftOperand", mandatory = true)
    private MoExpression leftOperand;
    @RoleDescriptor(type = ChildType.CHILD, role = "rightOperand", mandatory = true)
    private MoType rightOperand;

    public MoInstanceofExpression(String fileName, int startLine, int endLine, InstanceofExpression instanceofExpression) {
        super(fileName, startLine, endLine, instanceofExpression);
        moNodeType = MoNodeType.TYPEInstanceofExpression;
    }

    public void setLeftOperand(MoExpression leftOperand) {
        this.leftOperand = leftOperand;
    }

    public void setRightOperand(MoType rightOperand) {
        this.rightOperand = rightOperand;
    }

    public MoExpression getLeftOperand() {
        return leftOperand;
    }

    public MoType getRightOperand() {
        return rightOperand;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoInstanceofExpression(this);
    }

    @Override
    public List<MoNode> getChildren() {
        return List.of(leftOperand, rightOperand);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoInstanceofExpression, ?> description = descriptionsMap.get(role);
        if(description == leftOperandDescription) {
            return leftOperand;
        } else if(description == rightOperandDescription) {
            return rightOperand;
        } else {
            logger.error("Role {} not found in MoInstanceofExpression", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoInstanceofExpression, ?> description = descriptionsMap.get(role);
        if(description == leftOperandDescription) {
            setLeftOperand((MoExpression) value);
        } else if(description == rightOperandDescription) {
            setRightOperand((MoType) value);
        } else {
            logger.error("Role {} not found in MoInstanceofExpression", role);
        }
    }

    public static Map<String, Description<MoInstanceofExpression, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoInstanceofExpression(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoInstanceofExpression otherInstanceofExpression) {
            return leftOperand.isSame(otherInstanceofExpression.leftOperand) &&
                    rightOperand.isSame(otherInstanceofExpression.rightOperand);
        }
        return false;
    }
}
