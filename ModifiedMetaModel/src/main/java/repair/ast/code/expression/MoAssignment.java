package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.Assignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.virtual.MoAssignmentOperator;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.Map;

public class MoAssignment extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoAssignment.class);
    @Serial
    private static final long serialVersionUID = -3011203511611974969L;

    private final static Description<MoAssignment, MoExpression> leftHandSideDescription =
            new Description<>(ChildType.CHILD, MoAssignment.class, MoExpression.class,
                    "leftHandSide", true);

    private final static Description<MoAssignment, MoAssignmentOperator> operatorDescription =
            new Description<>(ChildType.CHILD, MoAssignment.class, MoAssignmentOperator.class,
                    "operator", true);

    private final static Description<MoAssignment, MoExpression> rightHandSideDescription =
            new Description<>(ChildType.CHILD, MoAssignment.class, MoExpression.class,
                    "rightHandSide", true);

    private final static Map<String, Description<MoAssignment, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("leftHandSide", leftHandSideDescription),
            Map.entry("operator", operatorDescription),
            Map.entry("rightHandSide", rightHandSideDescription)
    );


    @RoleDescriptor(type = ChildType.CHILD, role = "leftHandSide", mandatory = true)
    private MoExpression left = null;

    @RoleDescriptor(type = ChildType.CHILD, role = "operator", mandatory = true)
    private MoAssignmentOperator operator;

    @RoleDescriptor(type = ChildType.CHILD, role = "rightHandSide", mandatory = true)
    private MoExpression right = null;


    public MoAssignment(String fileName, int startLine, int endLine, Assignment assignment) {
        super(fileName, startLine, endLine, assignment);
        moNodeType = MoNodeType.TYPEAssignment;
    }

    public void setLeft(MoExpression left) {
        this.left = left;
    }

    public void setRight(MoExpression right) {
        this.right = right;
    }

    public MoExpression getLeft() {
        return left;
    }
    public MoAssignmentOperator getOperator() {
        return operator;
    }

    public MoExpression getRight() {
        return right;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoAssignment(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoAssignment, ?> description = descriptionsMap.get(role);
        if(description == leftHandSideDescription) {
            return left;
        } else if(description == operatorDescription) {
            return operator;
        } else if(description == rightHandSideDescription) {
            return right;
        } else {
            logger.error("Role {} not found in MoAssignment", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoAssignment, ?> description = descriptionsMap.get(role);
        if(description == leftHandSideDescription) {
            left = (MoExpression) value;
        } else if(description == operatorDescription) {
            operator = (MoAssignmentOperator) value;
        } else if(description == rightHandSideDescription) {
            right = (MoExpression) value;
        } else {
            logger.error("Role {} not found in MoAssignment", role);
        }
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoAssignment(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if (other instanceof MoAssignment otherAssignment) {
            return left.isSame(otherAssignment.left) &&
                    operator.isSame(otherAssignment.operator) &&
                    right.isSame(otherAssignment.right);
        }
        return false;
    }
}
