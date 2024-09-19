package repair.ast.code;

import org.eclipse.jdt.core.dom.BlockComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.role.Description;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Map;

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
    public List<MoNode> getChildren() {
        return List.of();
    }

    @Override
    public boolean isLeaf() {
        return true;
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

    public static Map<String, Description<MoBlockComment, ?>> getDescriptionsMap() {
        logger.error("BlockComment does not have any structural property");
        return null;
    }

    @Override
    public Description<MoBlockComment, ?> getDescription(String role) {
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
