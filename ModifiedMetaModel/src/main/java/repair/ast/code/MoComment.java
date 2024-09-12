package repair.ast.code;

import org.eclipse.jdt.core.dom.Comment;
import repair.ast.MoNode;

import java.io.Serial;

public abstract class MoComment extends MoNode {
    @Serial
    private static final long serialVersionUID = 211329236630137169L;
    private String commentStr;

    /**
     * Comment is the super class of LineComment, BlockComment and Javadoc
     *
     */
    public MoComment(String fileName, int startLine, int endLine, Comment comment) {
        super(fileName, startLine, endLine, comment);
    }

    public String getCommentStr() {
        return commentStr;
    }

    public void setCommentStr(String commentStr) {
        this.commentStr = commentStr;
    }

    @Override
    public String toString() {
        return commentStr;
    }

}
