package repair.ast.code.virtual;

import org.eclipse.jdt.core.dom.ASTNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoExpression;
import repair.ast.code.expression.MoPrefixExpression;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Map;

public class MoPrefixOperator extends MoVirtualChildNode {
    private static final Logger logger = LoggerFactory.getLogger(MoPrefixOperator.class);
    @Serial
    private static final long serialVersionUID = 8222472486393252313L;

    private final static Description<MoPrefixOperator, OperatorKind> operatorDescription =
            new Description<>(ChildType.SIMPLE, MoPrefixOperator.class, OperatorKind.class,
                    "operator", true);

    private final static Map<String, Description<MoPrefixOperator, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("operator", operatorDescription)
    );

    @RoleDescriptor(type = ChildType.SIMPLE, role = "operator", mandatory = true)
    private OperatorKind operator;

    public MoPrefixOperator(String fileName, int startLine, int endLine, int elementPos, int elementLength, ASTNode oriNode) {
        super(fileName, startLine, endLine, elementPos, elementLength, null);
        moNodeType = MoNodeType.TYPEPrefixOperator;
    }

    @Override
    public List<MoNode> getChildren() {
        return List.of();
    }

    @Override
    public boolean isLeaf() {
        return true;
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

    public OperatorKind getOperator() {
        return operator;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoPrefixOperator, ?> description = descriptionsMap.get(role);
        if(description == operatorDescription) {
            return operator;
        } else {
            logger.error("Role {} not found in MoPrefixOperator", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoPrefixOperator, ?> description = descriptionsMap.get(role);
        if(description == operatorDescription) {
            this.operator = OperatorKind.fromCode((String) value);
        } else {
            logger.error("Role {} not found in MoPrefixOperator", role);
        }
    }

    public static Map<String, Description<MoPrefixOperator, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoPrefixOperator clone = new MoPrefixOperator(getFileName(), getStartLine(), getEndLine(), getElementPos(), getElementLength(),null);
        clone.setStructuralProperty("operator", operator.toString());
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoPrefixOperator otherPrefixOperator) {
            return operator.equals(otherPrefixOperator.operator);
        }
        return false;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoPrefixOperator(this);
    }
}
