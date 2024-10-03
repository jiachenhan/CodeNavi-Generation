package repair.ast.code.type;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Type;
import repair.ast.MoNode;

import java.io.Serial;
import java.nio.file.Path;

public abstract class MoType extends MoNode {
    @Serial
    private static final long serialVersionUID = -4239778955026334926L;

    public MoType(Path fileName, int startLine, int endLine, Type typeNode) {
        super(fileName, startLine, endLine, typeNode);
    }
}
