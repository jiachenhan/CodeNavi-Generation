package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Name;
import repair.ast.code.MoDocElement;

import java.io.Serial;
import java.nio.file.Path;

public abstract class MoName extends MoExpression implements MoDocElement {
    @Serial
    private static final long serialVersionUID = 6071550553170179615L;

    protected MoName(Path fileName, int startLine, int endLine, Name name) {
        super(fileName, startLine, endLine, name);
    }

    public abstract String getIdentifier();

    // todo: nameBinding?
}
