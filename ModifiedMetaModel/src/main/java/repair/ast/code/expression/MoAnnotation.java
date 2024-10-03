package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.Expression;
import repair.ast.MoNode;
import repair.ast.code.MoExtendedModifier;
import repair.ast.code.expression.MoExpression;
import repair.ast.role.ChildType;
import repair.ast.role.RoleDescriptor;

import java.io.Serial;
import java.nio.file.Path;

public abstract class MoAnnotation extends MoExpression implements MoExtendedModifier {
    @Serial
    private static final long serialVersionUID = 8833998839560871356L;

    @RoleDescriptor(type = ChildType.CHILD, role = "typeName", mandatory = true)
    protected MoName typeName;

    public MoAnnotation(Path fileName, int startLine, int endLine, Annotation annotation) {
        super(fileName, startLine, endLine, annotation);
    }

    public void setTypeName(MoName typeName) {
        this.typeName = typeName;
    }

    public MoName getTypeName() {
        return typeName;
    }

    @Override
    public boolean isModifier() {
        return false;
    }

    @Override
    public boolean isAnnotation() {
        return true;
    }
}
