package repair.ast.code;

import org.eclipse.jdt.core.dom.LineComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.statement.MoVariableDeclarationStatement;
import repair.ast.role.Description;
import repair.ast.visitor.Visitor;

import java.io.Serial;

public class MoLineComment extends MoComment {
    private static final Logger logger = LoggerFactory.getLogger(MoLineComment.class);
    @Serial
    private static final long serialVersionUID = 9171969145584851370L;

    public MoLineComment(String fileName, int startLine, int endLine, LineComment lineComment) {
        super(fileName, startLine, endLine, lineComment);
        moNodeType = MoNodeType.TYPELineComment;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoLineComment(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        logger.error("LineComment does not have any structural property");
        return null;
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        logger.error("LineComment does not have any structural property");
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        logger.error("LineComment does not have any structural property");
        return null;
    }

    @Override
    public MoNode shallowClone() {
        MoLineComment clone = new MoLineComment(getFileName(), getStartLine(), getEndLine(), null);
        clone.setCommentStr(getCommentStr());
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if (other instanceof MoLineComment moLineComment) {
            return getCommentStr().equals(moLineComment.getCommentStr());
        }
        return false;
    }
}
