package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoSimpleName;
import repair.ast.code.type.MoType;
import repair.ast.declaration.MoFieldDeclaration;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.Map;

public class MoLabeledStatement extends MoStatement {
    private static final Logger logger = LoggerFactory.getLogger(MoLabeledStatement.class);
    @Serial
    private static final long serialVersionUID = 958078583935315658L;

    private final static Description<MoLabeledStatement, MoSimpleName> labelDescription =
            new Description<>(ChildType.CHILD, MoLabeledStatement.class, MoSimpleName.class,
                    "label", true);

    private final static Description<MoLabeledStatement, MoStatement> bodyDescription =
            new Description<>(ChildType.CHILD, MoLabeledStatement.class, MoStatement.class,
                    "body", true);

    private final static Map<String, Description<MoLabeledStatement, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("label", labelDescription),
            Map.entry("body", bodyDescription)
    );


    @RoleDescriptor(type = ChildType.CHILD, role = "label", mandatory = true)
    private MoSimpleName label;
    @RoleDescriptor(type = ChildType.CHILD, role = "body", mandatory = true)
    private MoStatement statement;

    public MoLabeledStatement(String fileName, int startLine, int endLine, LabeledStatement labeledStatement) {
        super(fileName, startLine, endLine, labeledStatement);
        moNodeType = MoNodeType.TYPELabeledStatement;
    }

    public void setLabel(MoSimpleName label) {
        this.label = label;
    }

    public void setStatement(MoStatement statement) {
        this.statement = statement;
    }

    public MoSimpleName getLabel() {
        return label;
    }

    public MoStatement getStatement() {
        return statement;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoLabeledStatement(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoLabeledStatement, ?> description = descriptionsMap.get(role);
        if(description == labelDescription) {
            return label;
        } else if(description == bodyDescription) {
            return statement;
        } else {
            logger.error("Role {} not found in MoLabeledStatement", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoLabeledStatement, ?> description = descriptionsMap.get(role);
        if(description == labelDescription) {
            label = (MoSimpleName) value;
        } else if(description == bodyDescription) {
            statement = (MoStatement) value;
        } else {
            logger.error("Role {} not found in MoLabeledStatement", role);
        }
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoLabeledStatement(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoLabeledStatement otherLabeledStatement) {
            return label.isSame(otherLabeledStatement.label) &&
                    statement.isSame(otherLabeledStatement.statement);
        }
        return false;
    }
}
