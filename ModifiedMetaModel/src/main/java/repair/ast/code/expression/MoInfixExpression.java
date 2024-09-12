package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.MoExtendedModifier;
import repair.ast.code.MoJavadoc;
import repair.ast.code.type.MoPrimitiveType;
import repair.ast.code.type.MoType;
import repair.ast.declaration.MoFieldDeclaration;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MoInfixExpression extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoInfixExpression.class);
    @Serial
    private static final long serialVersionUID = -5065102558828940531L;

    private final static Description<MoInfixExpression, MoExpression> leftOperandDescription =
            new Description<>(ChildType.CHILD, MoInfixExpression.class, MoExpression.class,
                    "leftOperand", false);

    private final static Description<MoInfixExpression, MoInfixExpression.OperatorKind> operatorDescription =
            new Description<>(ChildType.SIMPLE, MoInfixExpression.class, MoInfixExpression.OperatorKind.class,
                    "operator", true);

    private final static Description<MoInfixExpression, MoExpression> rightOperandDescription =
            new Description<>(ChildType.CHILD, MoInfixExpression.class, MoExpression.class,
                    "rightOperand", true);

    private final static Description<MoInfixExpression, MoExpression> extendedOperandsDescription =
            new Description<>(ChildType.CHILDLIST, MoInfixExpression.class, MoExpression.class,
                    "extendedOperands", true);

    private final static Map<String, Description<MoInfixExpression, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("leftOperand", leftOperandDescription),
            Map.entry("operator", operatorDescription),
            Map.entry("rightOperand", rightOperandDescription),
            Map.entry("extendedOperands", extendedOperandsDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "leftOperand", mandatory = true)
    private MoExpression left;
    @RoleDescriptor(type = ChildType.SIMPLE, role = "operator", mandatory = true)
    private MoInfixExpression.OperatorKind operator;
    @RoleDescriptor(type = ChildType.CHILD, role = "rightOperand", mandatory = true)
    private MoExpression right;
    @RoleDescriptor(type = ChildType.CHILDLIST, role = "extendedOperands", mandatory = true)
    private final MoNodeList<MoExpression> extendedOperands;

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoInfixExpression otherInfix) {
            return left.isSame(otherInfix.left) && operator.equals(otherInfix.operator) &&
                    right.isSame(otherInfix.right) && MoNodeList.sameList(extendedOperands, otherInfix.extendedOperands);
        }
        return false;
    }

    public enum OperatorKind{
        TIMES("*"),
        DIVIDE("/"),
        REMAINDER("%"),
        PLUS("+"),
        MINUS("-"),
        LEFT_SHIFT("<<"),
        RIGHT_SHIFT_SIGNED(">>"),
        RIGHT_SHIFT_UNSIGNED(">>>"),
        LESS("<"),
        GREATER(">"),
        LESS_EQUALS("<="),
        GREATER_EQUALS(">="),
        EQUALS("=="),
        NOT_EQUALS("!="),
        XOR("^"),
        AND("&"),
        OR("|"),
        CONDITIONAL_AND("&&"),
        CONDITIONAL_OR("||");

        private final String keyword;
        OperatorKind(String operator){
            this.keyword = operator;
        }

        public static OperatorKind fromCode(String value) {
            for (OperatorKind operatorKind : OperatorKind.values()) {
                if (operatorKind.keyword.equals(value)) {
                    return operatorKind;
                }
            }
            throw new IllegalArgumentException("No enum constant for operator: " + value);
        }

        @Override
        public String toString(){
            return keyword;
        }
    }

    public MoInfixExpression(String fileName, int startLine, int endLine, InfixExpression infixExpression) {
        super(fileName, startLine, endLine, infixExpression);
        moNodeType = MoNodeType.TYPEInfixExpression;
        extendedOperands = new MoNodeList<>(this, extendedOperandsDescription);
    }

    public void setLeft(MoExpression left) {
        this.left = left;
    }

    public void setOperator(OperatorKind operator) {
        this.operator = operator;
    }

    public void setRight(MoExpression right) {
        this.right = right;
    }

    public void addExtendedOperand(MoExpression operand) {
        this.extendedOperands.add(operand);
    }

    public MoExpression getLeft() {
        return left;
    }

    public MoInfixExpression.OperatorKind getOperator() {
        return operator;
    }

    public MoExpression getRight() {
        return right;
    }

    public MoNodeList<MoExpression> getExtendedOperands() {
        return extendedOperands;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoInfixExpression(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoInfixExpression, ?> description = descriptionsMap.get(role);
        if(description == leftOperandDescription) {
            return left;
        } else if(description == operatorDescription) {
            return operator;
        } else if(description == rightOperandDescription) {
            return right;
        } else if(description == extendedOperandsDescription) {
            return extendedOperands;
        } else {
            logger.error("Role {} not found in MoInfixExpression", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoInfixExpression, ?> description = descriptionsMap.get(role);
        if(description == leftOperandDescription) {
            left = (MoExpression) value;
        } else if(description == operatorDescription) {
            operator = MoInfixExpression.OperatorKind.fromCode((String)value);
        } else if(description == rightOperandDescription) {
            right = (MoExpression) value;
        } else if(description == extendedOperandsDescription) {
            extendedOperands.clear();
            extendedOperands.addAll((List<MoExpression>) value);
        } else {
            logger.error("Role {} not found in MoInfixExpression", role);
        }
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoInfixExpression clone = new MoInfixExpression(getFileName(), getStartLine(), getEndLine(), null);
        clone.setStructuralProperty("operator", getOperator().toString());
        return clone;
    }

}
