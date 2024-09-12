package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.MethodReference;
import repair.ast.MoNodeList;
import repair.ast.code.type.MoType;
import repair.ast.role.ChildType;
import repair.ast.role.RoleDescriptor;

import java.io.Serial;
import java.util.List;

public abstract class MoMethodReference extends MoExpression {
    @Serial
    private static final long serialVersionUID = -8681000619339531947L;

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "typeArguments", mandatory = true)
    protected MoNodeList<MoType> typeArguments;

    protected MoMethodReference(String fileName, int startLine, int endLine, MethodReference methodReference) {
        super(fileName, startLine, endLine, methodReference);
    }

    public void addTypeArgument(MoType type) {
        typeArguments.add(type);
    }

    public List<MoType> getTypeArguments() {
        return typeArguments;
    }
}
