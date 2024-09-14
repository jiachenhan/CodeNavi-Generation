package repair.ast.code.virtual;

import org.eclipse.jdt.core.dom.ASTNode;
import repair.ast.MoNode;

import java.io.Serial;

/**
 * virtual nodes for better match in gumtree
 */
public abstract class MoVirtualNode extends MoNode {
    @Serial
    private static final long serialVersionUID = 7371327390030467463L;

    public MoVirtualNode(String fileName, int startLine, int endLine, ASTNode oriNode) {
        super(fileName, startLine, endLine, null);
    }
}
