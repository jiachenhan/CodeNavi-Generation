package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.IfStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.MoExtendedModifier;
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
import java.util.Optional;

public class MoIfStatement extends MoStatement {
    private static final Logger logger = LoggerFactory.getLogger(MoIfStatement.class);
    @Serial
    private static final long serialVersionUID = 8214444217219138906L;

    private final static Description<MoIfStatement, MoExpression> expressionDescription =
            new Description<>(ChildType.CHILD, MoIfStatement.class, MoExpression.class,
                    "expression", true);

    private final static Description<MoIfStatement, MoStatement> thenStatementDescription =
            new Description<>(ChildType.CHILD, MoIfStatement.class, MoStatement.class,
                    "thenStatement", true);

    private final static Description<MoIfStatement, MoStatement> elseStatementDescription =
            new Description<>(ChildType.CHILD, MoIfStatement.class, MoStatement.class,
                    "elseStatement", false);

    private final static Map<String, Description<MoIfStatement, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("expression", expressionDescription),
            Map.entry("thenStatement", thenStatementDescription),
            Map.entry("elseStatement", elseStatementDescription)
    );


    @RoleDescriptor(type = ChildType.CHILD, role = "expression", mandatory = true)
    private MoExpression condition;
    @RoleDescriptor(type = ChildType.CHILD, role = "thenStatement", mandatory = true)
    private MoStatement thenStatement;
    @RoleDescriptor(type = ChildType.CHILD, role = "elseStatement", mandatory = false)
    private MoStatement elseStatement;

    public MoIfStatement(String fileName, int startLine, int endLine, IfStatement ifStatement) {
        super(fileName, startLine, endLine, ifStatement);
        moNodeType = MoNodeType.TYPEIfStatement;
    }

    public void setCondition(MoExpression condition) {
        this.condition = condition;
    }

    public void setThenStatement(MoStatement thenStatement) {
        this.thenStatement = thenStatement;
    }

    public void setElseStatement(MoStatement elseStatement) {
        this.elseStatement = elseStatement;
    }

    public MoExpression getCondition() {
        return condition;
    }

    public MoStatement getThenStatement() {
        return thenStatement;
    }

    public Optional<MoStatement> getElseStatement() {
        return Optional.ofNullable(elseStatement);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoIfStatement(this);
    }

    @Override
    public List<MoNode> getChildren() {
        if(elseStatement != null) {
            return List.of(condition, thenStatement, elseStatement);
        }
        return List.of(condition, thenStatement);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoIfStatement, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            return condition;
        } else if(description == thenStatementDescription) {
            return thenStatement;
        } else if(description == elseStatementDescription) {
            return elseStatement;
        } else {
            logger.error("Role {} not found in MoIfStatement", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoIfStatement, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            condition = (MoExpression) value;
        } else if(description == thenStatementDescription) {
            thenStatement = (MoStatement) value;
        } else if(description == elseStatementDescription) {
            elseStatement = (MoStatement) value;
        } else {
            logger.error("Role {} not found in MoIfStatement", role);
        }
    }

    public static Map<String, Description<MoIfStatement, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoIfStatement(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoIfStatement otherIfStatement) {
            return condition.isSame(otherIfStatement.condition) &&
                    thenStatement.isSame(otherIfStatement.thenStatement) &&
                    elseStatement.isSame(otherIfStatement.elseStatement);
        }
        return false;
    }
}
