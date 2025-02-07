package repair.dsl.kirin.map.code.node;

import repair.ast.MoNode;
import repair.ast.code.virtual.MoAssignmentOperator;
import repair.ast.code.virtual.MoInfixOperator;
import repair.ast.code.virtual.MoPostfixOperator;
import repair.ast.code.virtual.MoPrefixOperator;
import repair.dsl.kirin.map.code.Nameable;

public class Operator extends DSLNode implements Nameable {
    @Override
    public String prettyPrint() {
        throw new RuntimeException("should not happened");
    }

    @Override
    public NameAttr getNameAttr(MoNode node) {
        if (node instanceof MoPostfixOperator postfixOperator) {
            return new NameAttr("operator", postfixOperator.getOperator().toString(), true);
        } else if (node instanceof MoAssignmentOperator assignmentOperator) {
            return new NameAttr("operator", assignmentOperator.getOperator().toString(), true);
        } else if (node instanceof MoInfixOperator infixOperator) {
            return new NameAttr("operator", infixOperator.getOperator().toString(), true);
        } else if (node instanceof MoPrefixOperator prefixOperator) {
            return new NameAttr("operator", prefixOperator.getOperator().toString(), true);
        }
        throw new RuntimeException("should not happened");
    }
}
