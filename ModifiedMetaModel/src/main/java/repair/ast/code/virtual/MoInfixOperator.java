package repair.ast.code.virtual;

import org.eclipse.jdt.core.dom.ASTNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoAssignment;
import repair.ast.code.expression.MoInfixExpression;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.Map;

public class MoInfixOperator extends MoVirtualChildNode {
    private static final Logger logger = LoggerFactory.getLogger(MoInfixOperator.class);
    @Serial
    private static final long serialVersionUID = 2310274187045678708L;

    private final static Description<MoInfixOperator, OperatorKind> operatorDescription =
            new Description<>(ChildType.SIMPLE, MoInfixOperator.class, OperatorKind.class,
                    "operator", true);

    private final static Map<String, Description<MoInfixOperator, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("operator", operatorDescription)
    );

    @RoleDescriptor(type = ChildType.SIMPLE, role = "operator", mandatory = true)
    private OperatorKind operator;

    public MoInfixOperator(String fileName, int startLine, int endLine, ASTNode oriNode) {
        super(fileName, startLine, endLine, null);
        moNodeType = MoNodeType.TYPEInfixOperator;
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

    public OperatorKind getOperator() {
        return operator;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoInfixOperator, ?> description = descriptionsMap.get(role);
        if(description == operatorDescription) {
            return operator;
        } else {
            logger.error("Role {} not found in MoInfixOperator", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoInfixOperator, ?> description = descriptionsMap.get(role);
        if(description == operatorDescription) {
            operator = OperatorKind.fromCode((String)value);
        } else {
            logger.error("Role {} not found in MoInfixOperator", role);
        }
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoInfixOperator clone = new MoInfixOperator(getFileName(), getStartLine(), getEndLine(), null);
        clone.setStructuralProperty("operator", operator.toString());
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoInfixOperator otherInfix) {
            return operator.equals(otherInfix.operator);
        }
        return false;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoInfixOperator(this);
    }
}
