package repair.dsl.kirin.query;

import repair.ast.MoNode;
import repair.dsl.kirin.expr.Rhs;
import repair.dsl.kirin.map.code.node.DSLNode;

public class SubQuery extends NormalQuery implements Rhs {
    public SubQuery(MoNode referenceNode, DSLNode dslNode) {
        super(referenceNode, dslNode);
    }


}
