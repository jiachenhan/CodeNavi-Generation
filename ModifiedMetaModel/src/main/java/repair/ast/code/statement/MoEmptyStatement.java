package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.role.Description;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class MoEmptyStatement extends MoStatement {
    private static final Logger logger = LoggerFactory.getLogger(MoEmptyStatement.class);
    @Serial
    private static final long serialVersionUID = 8469060971594045593L;

    public MoEmptyStatement(Path fileName, int startLine, int endLine, EmptyStatement emptyStatement) {
        super(fileName, startLine, endLine, emptyStatement);
        moNodeType = MoNodeType.TYPEEmptyStatement;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoEmptyStatement(this);
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
        logger.error("MoEmptyStatement does not have any structural property.");
        return null;
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        logger.error("MoEmptyStatement does not have any structural property.");
    }

    public static Map<String, Description<MoEmptyStatement, ?>> getDescriptionsMap() {
        logger.error("MoEmptyStatement does not have any structural property.");
        return null;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        logger.error("MoEmptyStatement does not have any structural property.");
        return null;
    }

    @Override
    public MoNode shallowClone() {
        return new MoEmptyStatement(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        return other instanceof MoEmptyStatement;
    }
}
