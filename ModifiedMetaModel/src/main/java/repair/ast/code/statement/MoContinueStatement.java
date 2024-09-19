package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.ContinueStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoCompilationUnit;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoSimpleName;
import repair.ast.declaration.MoPackageDeclaration;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoContinueStatement extends MoStatement {
    private static final Logger logger = LoggerFactory.getLogger(MoContinueStatement.class);
    @Serial
    private static final long serialVersionUID = -3942070795133168899L;

    private final static Description<MoContinueStatement, MoSimpleName> continueLabelDescription =
            new Description<>(ChildType.CHILD, MoContinueStatement.class, MoSimpleName.class,
                    "label", false);

    private final static Map<String, Description<MoContinueStatement, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("label", continueLabelDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "label", mandatory = false)
    private MoSimpleName continueLabel;

    public MoContinueStatement(String fileName, int startLine, int endLine, ContinueStatement continueStatement) {
        super(fileName, startLine, endLine, continueStatement);
        moNodeType = MoNodeType.TYPEContinueStatement;
    }

    public void setContinueLabel(MoSimpleName continueLabel) {
        this.continueLabel = continueLabel;
    }

    public Optional<MoSimpleName> getContinueLabel() {
        return Optional.ofNullable(continueLabel);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoContinueStatement(this);
    }

    @Override
    public List<MoNode> getChildren() {
        if(continueLabel != null) {
            return List.of(continueLabel);
        }
        return List.of();
    }

    @Override
    public boolean isLeaf() {
        return continueLabel == null;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoContinueStatement, ?> description = descriptionsMap.get(role);
        if(description == continueLabelDescription) {
            return continueLabel;
        } else {
            logger.error("Role {} not found in MoContinueStatement", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoContinueStatement, ?> description = descriptionsMap.get(role);
        if(description == continueLabelDescription) {
            this.continueLabel = (MoSimpleName) value;
        } else {
            logger.error("Role {} not found in MoContinueStatement", role);
        }
    }

    public static Map<String, Description<MoContinueStatement, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoContinueStatement(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoContinueStatement otherContinueStatement) {
            if(continueLabel == null && otherContinueStatement.continueLabel == null) {
                return true;
            } else if(continueLabel != null && otherContinueStatement.continueLabel != null) {
                return continueLabel.isSame(otherContinueStatement.continueLabel);
            }
        }
        return false;
    }
}
