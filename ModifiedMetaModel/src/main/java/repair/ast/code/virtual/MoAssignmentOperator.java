package repair.ast.code.virtual;

import org.eclipse.jdt.core.dom.ASTNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class MoAssignmentOperator extends MoVirtualChildNode {
    private static final Logger logger = LoggerFactory.getLogger(MoAssignmentOperator.class);
    @Serial
    private static final long serialVersionUID = 304927332018868872L;

    private final static Description<MoAssignmentOperator, OperatorKind> operatorDescription =
            new Description<>(ChildType.SIMPLE, MoAssignmentOperator.class, OperatorKind.class,
                    "operator", true);

    private final static Map<String, Description<MoAssignmentOperator, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("operator", operatorDescription)
    );

    @RoleDescriptor(type = ChildType.SIMPLE, role = "operator", mandatory = true)
    private OperatorKind operator;

    public MoAssignmentOperator(Path fileName, int startLine, int endLine, int elementPos, int elementLength, ASTNode oriNode) {
        super(fileName, startLine, endLine, elementPos, elementLength, null);
        moNodeType = MoNodeType.TYPEAssigmentOperator;
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

        public static OperatorKind fromCode(String value) {
            for (OperatorKind operatorKind : OperatorKind.values()) {
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

    public OperatorKind getOperator() {
        return operator;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoAssignmentOperator, ?> description = descriptionsMap.get(role);
        if(description == operatorDescription) {
            return operator;
        } else {
            logger.error("Role {} not found in MoAssignmentOperator", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoAssignmentOperator, ?> description = descriptionsMap.get(role);
        if(description == operatorDescription) {
            operator = OperatorKind.fromCode((String)value);
        } else {
            logger.error("Role {} not found in MoAssignmentOperator", role);
        }
    }

    public static Map<String, Description<MoAssignmentOperator, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoAssignmentOperator clone = new MoAssignmentOperator(getFileName(), getStartLine(), getEndLine(), getElementPos(), getElementLength(), null);
        clone.setStructuralProperty("operator", operator.toString());
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoAssignmentOperator otherAssignmentOperator) {
            return operator.equals(otherAssignmentOperator.operator);
        }
        return false;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoAssignmentOperator(this);
    }
}
