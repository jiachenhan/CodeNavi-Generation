package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import repair.ast.MoNode;

import java.io.Serial;


public abstract class MoExpression extends MoNode {
    @Serial
    private static final long serialVersionUID = -2128799087068278887L;

    protected MoExpression(String fileName, int startLine, int endLine, Expression expression) {
        super(fileName, startLine, endLine, expression, null);
    }

    // todo: typeBinding??
}