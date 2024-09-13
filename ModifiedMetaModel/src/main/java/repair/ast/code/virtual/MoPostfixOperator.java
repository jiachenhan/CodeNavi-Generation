package repair.ast.code.virtual;

import org.eclipse.jdt.core.dom.ASTNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoPostfixExpression;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.Map;

public class MoPostfixOperator extends MoVirtualChildNode {
    private static final Logger logger = LoggerFactory.getLogger(MoPostfixOperator.class);
    @Serial
    private static final long serialVersionUID = 3138510275899154103L;

    private final static Description<MoPostfixOperator, OperatorKind> operatorDescription =
            new Description<>(ChildType.SIMPLE, MoPostfixOperator.class, OperatorKind.class,
                    "operator", true);

    private final static Map<String, Description<MoPostfixOperator, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("operator", operatorDescription)
    );

    @RoleDescriptor(type = ChildType.SIMPLE, role = "operator", mandatory = true)
    private OperatorKind operator;

    public MoPostfixOperator(String fileName, int startLine, int endLine, ASTNode oriNode) {
        super(fileName, startLine, endLine, null);
        moNodeType = MoNodeType.TYPEPostfixOperator;
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

    public OperatorKind getOperator() {
        return operator;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoPostfixOperator, ?> description = descriptionsMap.get(role);
        if(description == operatorDescription) {
            return operator;
        } else {
            logger.error("Role {} not found in MoAssignmentOperator", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoPostfixOperator, ?> description = descriptionsMap.get(role);
        if(description == operatorDescription) {
            operator = OperatorKind.fromCode((String)value);
        } else {
            logger.error("Role {} not found in MoAssignmentOperator", role);
        }
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoPostfixOperator clone = new MoPostfixOperator(getFileName(), getStartLine(), getEndLine(), null);
        clone.setStructuralProperty("operator", operator.toString());
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoPostfixOperator otherPostfixOperator) {
            return operator.equals(otherPostfixOperator.operator);
        }
        return false;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoPostfixOperator(this);
    }
}
