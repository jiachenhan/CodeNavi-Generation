package repair.ast.code;

import org.eclipse.jdt.core.dom.CatchClause;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.statement.MoBlock;
import repair.ast.declaration.MoAnonymousClassDeclaration;
import repair.ast.declaration.MoBodyDeclaration;
import repair.ast.declaration.MoSingleVariableDeclaration;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class MoCatchClause extends MoNode {
    private static final Logger logger = LoggerFactory.getLogger(MoCatchClause.class);
    @Serial
    private static final long serialVersionUID = 590134489964728928L;

    private final static Description<MoCatchClause, MoSingleVariableDeclaration> exceptionDescription =
            new Description<>(ChildType.CHILD, MoCatchClause.class, MoSingleVariableDeclaration.class,
                    "exception", true);

    private final static Description<MoCatchClause, MoBlock> bodyDescription =
            new Description<>(ChildType.CHILD, MoCatchClause.class, MoBlock.class,
                    "body", true);

    private final static Map<String, Description<MoCatchClause, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("exception", exceptionDescription),
            Map.entry("body", bodyDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "exception", mandatory = true)
    private MoSingleVariableDeclaration exception;

    @RoleDescriptor(type = ChildType.CHILD, role = "body", mandatory = true)
    private MoBlock body;

    public MoCatchClause(Path fileName, int startLine, int endLine, CatchClause catchClause) {
        super(fileName, startLine, endLine, catchClause);
        moNodeType = MoNodeType.TYPECatchClause;
    }

    public void setException(MoSingleVariableDeclaration exception) {
        this.exception = exception;
    }

    public void setBody(MoBlock body) {
        this.body = body;
    }

    public MoSingleVariableDeclaration getException() {
        return exception;
    }

    public MoBlock getBody() {
        return body;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoCatchClause(this);
    }

    @Override
    public List<MoNode> getChildren() {
        return List.of(exception, body);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoCatchClause, ?> description = descriptionsMap.get(role);
        if(description == exceptionDescription) {
            return exception;
        } else if(description == bodyDescription) {
            return body;
        } else {
            logger.error("Role {} not found in MoCatchClause", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoCatchClause, ?> description = descriptionsMap.get(role);
        if(description == exceptionDescription) {
            this.exception = (MoSingleVariableDeclaration) value;
        } else if(description == bodyDescription) {
            this.body = (MoBlock) value;
        } else {
            logger.error("Role {} not found in MoCatchClause", role);
            return;
        }
    }

    public static Map<String, Description<MoCatchClause, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoCatchClause(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoCatchClause moCatchClause) {
            return exception.isSame(moCatchClause.exception) && body.isSame(moCatchClause.body);
        }
        return false;
    }
}
