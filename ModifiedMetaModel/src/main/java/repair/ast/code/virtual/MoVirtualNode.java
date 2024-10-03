package repair.ast.code.virtual;

import org.eclipse.jdt.core.dom.ASTNode;
import repair.ast.MoNode;

import java.io.Serial;
import java.nio.file.Path;

/**
 * virtual nodes for better match in gumtree
 */
public abstract class MoVirtualNode extends MoNode {
    @Serial
    private static final long serialVersionUID = 7371327390030467463L;

    public MoVirtualNode(Path fileName, int startLine, int endLine, int elementPos, int elementLength, ASTNode oriNode) {
        super(fileName, startLine, endLine, elementPos, elementLength, null);
    }
}
