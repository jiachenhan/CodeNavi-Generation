package repair.ast.code.virtual;

import org.eclipse.jdt.core.dom.ASTNode;
import repair.ast.MoNode;

import java.io.Serial;

/**
 * Virtual nodes for MethodInvocationArguments
 * <p>
 * future support for more nodes.
 */
public abstract class MoVirtualChildListNode extends MoVirtualNode {
    @Serial
    private static final long serialVersionUID = 8960049876625630159L;

    public MoVirtualChildListNode(String fileName, int startLine, int endLine, ASTNode oriNode) {
        super(fileName, startLine, endLine, null);
    }
}
