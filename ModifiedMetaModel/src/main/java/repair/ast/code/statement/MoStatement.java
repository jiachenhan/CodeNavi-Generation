package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.Statement;
import repair.ast.MoNode;

import java.io.Serial;

public abstract class MoStatement extends MoNode {
    @Serial
    private static final long serialVersionUID = -970811580715182555L;

    public MoStatement(String fileName, int startLine, int endLine, Statement statement) {
        super(fileName, startLine, endLine, statement);
    }
}
