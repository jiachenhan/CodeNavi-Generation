package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.SwitchStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoExpression;
import repair.ast.code.type.MoType;
import repair.ast.declaration.MoFieldDeclaration;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Map;

public class MoSwitchStatement extends MoStatement {
    private static final Logger logger = LoggerFactory.getLogger(MoSwitchStatement.class);
    @Serial
    private static final long serialVersionUID = 546961971065410475L;

    private final static Description<MoSwitchStatement, MoExpression> expressionDescription =
            new Description<>(ChildType.CHILD, MoSwitchStatement.class, MoExpression.class,
                    "expression", true);

    private final static Description<MoSwitchStatement, MoStatement> statementsDescription =
            new Description<>(ChildType.CHILDLIST, MoSwitchStatement.class, MoStatement.class,
                    "statements", true);

    private final static Map<String, Description<MoSwitchStatement, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("expression", expressionDescription),
            Map.entry("statements", statementsDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "expression", mandatory = true)
    private MoExpression expression;
    @RoleDescriptor(type = ChildType.CHILDLIST, role = "statements", mandatory = true)
    private final MoNodeList<MoStatement> statements;

    public MoSwitchStatement(String fileName, int startLine, int endLine, SwitchStatement switchStatement) {
        super(fileName, startLine, endLine, switchStatement);
        moNodeType = MoNodeType.TYPESwitchStatement;
        statements = new MoNodeList<>(this, statementsDescription);
    }

    public void setExpression(MoExpression expression) {
        this.expression = expression;
    }

    public void addStatement(MoStatement statement) {
        statements.add(statement);
    }

    public MoExpression getExpression() {
        return expression;
    }

    public List<MoStatement> getStatements() {
        return statements;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoSwitchStatement(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoSwitchStatement, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            return expression;
        } else if(description == statementsDescription) {
            return statements;
        } else {
            logger.error("Role {} not found in MoSwitchStatement", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoSwitchStatement, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            this.expression = (MoExpression) value;
        } else if(description == statementsDescription) {
            this.statements.clear();
            this.statements.addAll((List<MoStatement>) value);
        } else {
            logger.error("Role {} not found in MoSwitchStatement", role);
        }
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoSwitchStatement(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoSwitchStatement moSwitchStatement) {
            boolean match = expression.isSame(moSwitchStatement.expression);
            match = match && MoNodeList.sameList(statements, moSwitchStatement.statements);
            return match;
        }
        return false;
    }
}
