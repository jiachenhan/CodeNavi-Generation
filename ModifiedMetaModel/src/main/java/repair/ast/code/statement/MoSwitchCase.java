package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.SwitchCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoExpression;
import repair.ast.declaration.MoFieldDeclaration;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoSwitchCase extends MoStatement {
    private static final Logger logger = LoggerFactory.getLogger(MoSwitchCase.class);
    @Serial
    private static final long serialVersionUID = -4372749863691255579L;

    private final static Description<MoSwitchCase, MoExpression> expressionDescription =
            new Description<>(ChildType.CHILD, MoSwitchCase.class, MoExpression.class,
                    "expression", true);

    private final static Map<String, Description<MoSwitchCase, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("expression", expressionDescription)
    );

    /**
     *  if the case is default, expression is null
      */
    @RoleDescriptor(type = ChildType.CHILD, role = "expression", mandatory = false)
    private MoExpression expression;

    public MoSwitchCase(Path fileName, int startLine, int endLine, SwitchCase switchCase) {
        super(fileName, startLine, endLine, switchCase);
        moNodeType = MoNodeType.TYPESwitchCase;
    }

    public void setExpression(MoExpression expression) {
        this.expression = expression;
    }

    public Optional<MoExpression> getExpression() {
        return Optional.ofNullable(expression);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoSwitchCase(this);
    }

    @Override
    public List<MoNode> getChildren() {
        if(expression != null) {
            return List.of(expression);
        }
        return List.of();
    }

    @Override
    public boolean isLeaf() {
        return expression == null;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoSwitchCase, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            return expression;
        } else {
            logger.error("Role {} not found in MoSwitchCase", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoSwitchCase, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            this.expression = (MoExpression) value;
        } else {
            logger.error("Role {} not found in MoSwitchCase", role);
        }
    }

    public static Map<String, Description<MoSwitchCase, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoSwitchCase(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoSwitchCase moSwitchCase) {
            if(expression == null) {
                return moSwitchCase.expression == null;
            } else {
                return expression.isSame(moSwitchCase.expression);
            }
        }
        return false;
    }
}
