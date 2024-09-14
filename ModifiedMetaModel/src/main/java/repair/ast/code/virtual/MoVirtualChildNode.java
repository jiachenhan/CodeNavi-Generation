package repair.ast.code.virtual;

import org.eclipse.jdt.core.dom.ASTNode;
import repair.ast.MoNode;

import java.io.Serial;

/**
 * Virtual nodes for infix, prefix, postfix, assignment
 * <p>
 * future support for tagName, typeDeclaration, etc.
 */
public abstract class MoVirtualChildNode extends MoVirtualNode {
    @Serial
    private static final long serialVersionUID = 474376386040701912L;

    public MoVirtualChildNode(String fileName, int startLine, int endLine, int elementPos, int elementLength, ASTNode oriNode) {
        super(fileName, startLine, endLine, elementPos, elementLength, null);
    }
}
