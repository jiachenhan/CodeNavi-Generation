package repair.dsl.kirin.map.code.node;

import repair.dsl.kirin.UnSupportException;

public class DSLUnSupportNode extends DSLNode {
    @Override
    public String prettyPrint() {
        throw new UnSupportException("should not happened");
    }

    @Override
    public String getAlias() {
        return "";
    }
}
