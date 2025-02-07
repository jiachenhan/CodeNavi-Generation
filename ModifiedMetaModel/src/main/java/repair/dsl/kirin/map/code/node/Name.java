package repair.dsl.kirin.map.code.node;

import repair.ast.MoNode;
import repair.ast.code.expression.MoName;
import repair.ast.code.expression.MoQualifiedName;
import repair.ast.code.expression.MoSimpleName;
import repair.dsl.kirin.UnSupportException;
import repair.dsl.kirin.map.code.Nameable;

public class Name extends DSLNode implements Nameable {
    @Override
    public String prettyPrint() {
        throw new UnSupportException("should not happened");
    }

    @Override
    public NameAttr getNameAttr(MoNode node) {
        if (node instanceof MoName) {
            if (node instanceof MoSimpleName simpleName) {
                return new NameAttr("name", simpleName.getIdentifier(), true);
            } else if (node instanceof MoQualifiedName qualifiedName) {
                return new NameAttr("name", qualifiedName.getName().getIdentifier(), true);
            }
        }
        throw new RuntimeException("should not happened");
    }
}
