package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoSimpleName;
import repair.ast.declaration.MoAnonymousClassDeclaration;
import repair.ast.declaration.MoBodyDeclaration;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoBreakStatement extends MoStatement{
    private static final Logger logger = LoggerFactory.getLogger(MoBreakStatement.class);
    @Serial
    private static final long serialVersionUID = 4174126600132794047L;

    private final static Description<MoBreakStatement, MoSimpleName> breakLabelDescription =
            new Description<>(ChildType.CHILD, MoBreakStatement.class, MoSimpleName.class,
                    "label", false);

    private final static Map<String, Description<MoBreakStatement, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("label", breakLabelDescription)
    );


    @RoleDescriptor(type = ChildType.CHILD, role = "label", mandatory = false)
    private MoSimpleName breakLabel;

    public MoBreakStatement(Path fileName, int startLine, int endLine, BreakStatement breakStatement) {
        super(fileName, startLine, endLine, breakStatement);
        moNodeType = MoNodeType.TYPEBreakStatement;
    }

    public void setBreakLabel(MoSimpleName breakLabel) {
        this.breakLabel = breakLabel;
    }

    public Optional<MoSimpleName> getBreakLabel() {
        return Optional.ofNullable(breakLabel);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoBreakStatement(this);
    }

    @Override
    public List<MoNode> getChildren() {
        if(breakLabel != null) {
            return List.of(breakLabel);
        } else {
            return List.of();
        }
    }

    @Override
    public boolean isLeaf() {
        return breakLabel == null;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoBreakStatement, ?> description = descriptionsMap.get(role);
        if(description == breakLabelDescription) {
            return breakLabel;
        } else {
            logger.error("Role {} not found in MoBreakStatement", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoBreakStatement, ?> description = descriptionsMap.get(role);
        if(description == breakLabelDescription) {
            this.breakLabel = (MoSimpleName) value;
        } else {
            logger.error("Role {} not found in MoBreakStatement", role);
            return;
        }
    }

    public static Map<String, Description<MoBreakStatement, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoBreakStatement(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoBreakStatement moBreakStatement) {
            return (this.breakLabel == null && moBreakStatement.breakLabel == null) ||
                    (this.breakLabel != null && this.breakLabel.isSame(moBreakStatement.breakLabel));
        }
        return false;
    }
}
