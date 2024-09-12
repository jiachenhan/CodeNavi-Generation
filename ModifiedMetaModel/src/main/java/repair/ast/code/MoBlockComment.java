package repair.ast.code;

import org.eclipse.jdt.core.dom.BlockComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.role.Description;
import repair.ast.visitor.Visitor;

import java.io.Serial;

public class MoBlockComment extends MoComment {
    private static final Logger logger = LoggerFactory.getLogger(MoBlockComment.class);
    @Serial
    private static final long serialVersionUID = 6521251584375479098L;

    public MoBlockComment(String fileName, int startLine, int endLine, BlockComment blockComment) {
        super(fileName, startLine, endLine, blockComment);
        moNodeType = MoNodeType.TYPEBlockComment;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoBlockComment(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        logger.error("BlockComment does not have any structural property");
        return null;
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        logger.error("BlockComment does not have any structural property");
        return;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        logger.error("BlockComment does not have any description");
        return null;
    }

    @Override
    public MoNode shallowClone() {
        MoBlockComment clone = new MoBlockComment(getFileName(), getStartLine(), getEndLine(), null);
        clone.setCommentStr(getCommentStr());
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if (other instanceof MoBlockComment moBlockComment) {
            return getCommentStr().equals(moBlockComment.getCommentStr());
        }
        return false;
    }
}
