package repair.dsl.kirin.map.code.node;

import repair.ast.MoNode;
import repair.ast.code.type.MoPrimitiveType;
import repair.dsl.kirin.map.code.Nameable;

public class PrimitiveType extends DSLNode implements Nameable {
    @Override
    public String prettyPrint() {
        throw new RuntimeException("should not happened");
    }

    @Override
    public NameAttr getNameAttr(MoNode node) {
        if (node instanceof MoPrimitiveType primitiveType) {
            return new NameAttr("name", primitiveType.getTypeKind().toString(), true);
        }
        throw new RuntimeException("should not happened");
    }

    @Override
    public String getAlias() {
        return "";
    }
}
