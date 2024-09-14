package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.ForStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoExpression;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoForStatement extends MoStatement {
    private static final Logger logger = LoggerFactory.getLogger(MoForStatement.class);
    @Serial
    private static final long serialVersionUID = 8257419942364267795L;

    private final static Description<MoForStatement, MoExpression> initializersDescription =
            new Description<>(ChildType.CHILDLIST, MoForStatement.class, MoExpression.class,
                    "initializers", true);

    private final static Description<MoForStatement, MoExpression> expressionDescription =
            new Description<>(ChildType.CHILD, MoForStatement.class, MoExpression.class,
                    "expression", false);

    private final static Description<MoForStatement, MoExpression> updatersDescription =
            new Description<>(ChildType.CHILDLIST, MoForStatement.class, MoExpression.class,
                    "updaters", true);

    private final static Description<MoForStatement, MoStatement> bodyDescription =
            new Description<>(ChildType.CHILD, MoForStatement.class, MoStatement.class,
                    "body", true);

    private final static Map<String, Description<MoForStatement, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("initializers", initializersDescription),
            Map.entry("expression", expressionDescription),
            Map.entry("updaters", updatersDescription),
            Map.entry("body", bodyDescription)
    );


    @RoleDescriptor(type = ChildType.CHILDLIST, role = "initializers", mandatory = true)
    private final MoNodeList<MoExpression> initializers;

    @RoleDescriptor(type = ChildType.CHILD, role = "expression", mandatory = false)
    private MoExpression condition;

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "updaters", mandatory = true)
    private final MoNodeList<MoExpression> updaters;

    @RoleDescriptor(type = ChildType.CHILD, role = "body", mandatory = true)
    private MoStatement body;

    public MoForStatement(String fileName, int startLine, int endLine, ForStatement forStatement) {
        super(fileName, startLine, endLine, forStatement);
        moNodeType = MoNodeType.TYPEForStatement;
        initializers = new MoNodeList<>(this, initializersDescription);
        updaters = new MoNodeList<>(this, updatersDescription);
    }

    public void addInitializer(MoExpression expression) {
        initializers.add(expression);
    }

    public void setCondition(MoExpression condition) {
        this.condition = condition;
    }

    public void addUpdater(MoExpression expression) {
        updaters.add(expression);
    }

    public void setBody(MoStatement body) {
        this.body = body;
    }

    public List<MoExpression> getInitializers() {
        return initializers;
    }

    public Optional<MoExpression> getCondition() {
        return Optional.ofNullable(condition);
    }

    public List<MoExpression> getUpdaters() {
        return updaters;
    }

    public MoStatement getBody() {
        return body;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoForStatement(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoForStatement, ?> description = descriptionsMap.get(role);
        if(description == initializersDescription) {
            return initializers;
        } else if(description == expressionDescription) {
            return condition;
        } else if(description == updatersDescription) {
            return updaters;
        } else if(description == bodyDescription) {
            return body;
        } else {
            logger.error("Role {} not found in MoForStatement", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoForStatement, ?> description = descriptionsMap.get(role);
        if(description == initializersDescription) {
            initializers.clear();
            initializers.addAll((MoNodeList<MoExpression>) value);
        } else if(description == expressionDescription) {
            condition = (MoExpression) value;
        } else if(description == updatersDescription) {
            updaters.clear();
            updaters.addAll((MoNodeList<MoExpression>) value);
        } else if(description == bodyDescription) {
            body = (MoStatement) value;
        } else {
            logger.error("Role {} not found in MoForStatement", role);
        }
    }

    public static Map<String, Description<MoForStatement, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoForStatement(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoForStatement otherForStatement) {
            boolean match = MoNodeList.sameList(initializers, otherForStatement.initializers);
            match = match && (condition == null && otherForStatement.condition == null ||
                    condition != null && condition.isSame(otherForStatement.condition));
            match = match && MoNodeList.sameList(updaters, otherForStatement.updaters);
            match = match && body.isSame(otherForStatement.body);
            return match;
        }
        return false;
    }
}
