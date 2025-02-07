package repair.dsl.kirin.query;

import repair.ast.MoNode;
import repair.dsl.kirin.map.code.node.DSLNode;
import repair.dsl.kirin.map.code.node.FunctionDeclaration;

public class TemplateQuery extends NormalQuery {
    public TemplateQuery(MoNode referenceNode) {
        super(referenceNode, new FunctionDeclaration());
    }

    @Override
    public String prettyPrint() {
        return super.prettyPrint() + ";";
    }
}
