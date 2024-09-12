package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.type.MoType;
import repair.ast.declaration.MoFieldDeclaration;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.Map;

public class MoPrefixExpression extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoPrefixExpression.class);
    @Serial
    private static final long serialVersionUID = -2782971528698470144L;

    private final static Description<MoPrefixExpression, MoPrefixExpression.OperatorKind> operatorDescription =
            new Description<>(ChildType.SIMPLE, MoPrefixExpression.class, MoPrefixExpression.OperatorKind.class,
                    "operator", true);

    private final static Description<MoPrefixExpression, MoExpression> operandDescription =
            new Description<>(ChildType.CHILD, MoPrefixExpression.class, MoExpression.class,
                    "operand", true);

    private final static Map<String, Description<MoPrefixExpression, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("operator", operatorDescription),
            Map.entry("operand", operandDescription)
    );

    @RoleDescriptor(type = ChildType.SIMPLE, role = "operator", mandatory = true)
    private MoPrefixExpression.OperatorKind operator;
    @RoleDescriptor(type = ChildType.CHILD, role = "operand", mandatory = true)
    private MoExpression operand;

    @Override
    public boolean isSame(MoNode other) {
        if (other instanceof MoPrefixExpression moPrefixExpression) {
            return moPrefixExpression.operator.equals(this.operator) && moPrefixExpression.operand.isSame(this.operand);
        }
        return false;
    }


    public enum OperatorKind{
        INCREMENT("++"),
        DECREMENT("--"),
        PLUS("+"),
        MINUS("-"),
        COMPLEMENT("~"),
        NOT("!");

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

    public MoPrefixExpression(String fileName, int startLine, int endLine, PrefixExpression prefixExpression) {
        super(fileName, startLine, endLine, prefixExpression);
        moNodeType = MoNodeType.TYPEPrefixExpression;
    }

    public OperatorKind getOperator() {
        return operator;
    }

    public void setOperand(MoExpression operand) {
        this.operand = operand;
    }

    public MoExpression getOperand() {
        return operand;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoPrefixExpression(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoPrefixExpression, ?> description = descriptionsMap.get(role);
        if(description == operatorDescription) {
            return operator;
        } else if(description == operandDescription) {
            return operand;
        } else {
            logger.error("Role {} not found in MoPrefixExpression", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoPrefixExpression, ?> description = descriptionsMap.get(role);
        if(description == operatorDescription) {
            this.operator = OperatorKind.fromCode((String) value);
        } else if(description == operandDescription) {
            this.operand = (MoExpression) value;
        } else {
            logger.error("Role {} not found in MoPrefixExpression", role);
        }
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoPrefixExpression clone = new MoPrefixExpression(getFileName(), getStartLine(), getEndLine(), null);
        clone.setStructuralProperty("operator", this.operator.toString());
        return clone;
    }

}
