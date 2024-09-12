package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.PostfixExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.Map;

public class MoPostfixExpression extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoPostfixExpression.class);
    @Serial
    private static final long serialVersionUID = -1370917360130747402L;

    private final static Description<MoPostfixExpression, MoExpression> operandDescription =
            new Description<>(ChildType.CHILD, MoPostfixExpression.class, MoExpression.class,
                    "operand", true);

    private final static Description<MoPostfixExpression, MoPostfixExpression.OperatorKind> operatorDescription =
            new Description<>(ChildType.CHILDLIST, MoPostfixExpression.class, MoPostfixExpression.OperatorKind.class,
                    "operator", true);

    private final static Map<String, Description<MoPostfixExpression, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("operand", operandDescription),
            Map.entry("operator", operatorDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "operand", mandatory = true)
    private MoExpression operand;
    @RoleDescriptor(type = ChildType.SIMPLE, role = "operator", mandatory = true)
    private MoPostfixExpression.OperatorKind operator;

    @Override
    public boolean isSame(MoNode other) {
        if (other instanceof MoPostfixExpression moPostfixExpression) {
            return moPostfixExpression.operator.equals(this.operator) && moPostfixExpression.operand.isSame(this.operand);
        }
        return false;
    }

    public enum OperatorKind{
        INCREMENT("++"),
        DECREMENT("--");

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

    public MoPostfixExpression(String fileName, int startLine, int endLine, PostfixExpression postfixExpression) {
        super(fileName, startLine, endLine, postfixExpression);
        moNodeType = MoNodeType.TYPEPostfixExpression;
    }

    public void setOperand(MoExpression operand) {
        this.operand = operand;
    }

    public MoExpression getOperand() {
        return operand;
    }

    public OperatorKind getOperator() {
        return operator;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoPostfixExpression(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoPostfixExpression, ?> description = descriptionsMap.get(role);
        if(description == operandDescription) {
            return operand;
        } else if(description == operatorDescription) {
            return operator;
        } else {
            logger.error("Role {} not found in MoPostfixExpression", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoPostfixExpression, ?> description = descriptionsMap.get(role);
        if(description == operandDescription) {
            this.operand = (MoExpression) value;
        } else if(description == operatorDescription) {
            this.operator = OperatorKind.fromCode((String) value);
        } else {
            logger.error("Role {} not found in MoPostfixExpression", role);
        }
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoPostfixExpression clone = new MoPostfixExpression(getFileName(), getStartLine(), getEndLine(), null);
        clone.setStructuralProperty("operator", getOperator().toString());
        return clone;
    }

}
