package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.Assignment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.MoModifier;
import repair.ast.declaration.MoAnonymousClassDeclaration;
import repair.ast.declaration.MoBodyDeclaration;
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

    private final static Description<MoAssignment, MoAssignment.OperatorKind> operatorDescription =
            new Description<>(ChildType.SIMPLE, MoAssignment.class, MoAssignment.OperatorKind.class,
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

    @RoleDescriptor(type = ChildType.SIMPLE, role = "operator", mandatory = true)
    private MoAssignment.OperatorKind operatorKind;

    @RoleDescriptor(type = ChildType.CHILD, role = "rightHandSide", mandatory = true)
    private MoExpression right = null;

    public enum OperatorKind{
        ASSIGN("="),
        PLUS_ASSIGN("+="),
        MINUS_ASSIGN("-="),
        TIMES_ASSIGN("*="),
        DIVIDE_ASSIGN("/="),
        REMAINDER_ASSIGN("%="),
        BIT_AND_ASSIGN("&="),
        BIT_OR_ASSIGN("|="),
        BIT_XOR_ASSIGN("^="),
        LEFT_SHIFT_ASSIGN("<<="),
        RIGHT_SHIFT_SIGNED_ASSIGN(">>="),
        RIGHT_SHIFT_UNSIGNED_ASSIGN(">>>=");

        private final String keyword;
        OperatorKind(String operator){
            this.keyword = operator;
        }

        public static MoAssignment.OperatorKind fromCode(String value) {
            for (MoAssignment.OperatorKind operatorKind : MoAssignment.OperatorKind.values()) {
                if (operatorKind.keyword.equals(value)) {
                    return operatorKind;
                }
            }
            throw new IllegalArgumentException("No enum constant for kind: " + value);
        }

        @Override
        public String toString(){
            return keyword;
        }

    }

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

    public MoAssignment.OperatorKind getOperatorKind() {
        return operatorKind;
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
            return operatorKind;
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
            operatorKind = OperatorKind.fromCode((String) value);
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
        MoAssignment clone = new MoAssignment(getFileName(), getStartLine(), getEndLine(), null);
        clone.setStructuralProperty("operator", getOperatorKind().toString());
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if (other instanceof MoAssignment otherAssignment) {
            return left.isSame(otherAssignment.left) &&
                    operatorKind.equals(otherAssignment.operatorKind) &&
                    right.isSame(otherAssignment.right);
        }
        return false;
    }
}
