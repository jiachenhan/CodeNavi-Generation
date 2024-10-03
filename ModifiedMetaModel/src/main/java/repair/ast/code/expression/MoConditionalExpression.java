package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoCompilationUnit;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.declaration.MoPackageDeclaration;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class MoConditionalExpression extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoConditionalExpression.class);
    @Serial
    private static final long serialVersionUID = 7673823823679521112L;

    private final static Description<MoConditionalExpression, MoExpression> conditionDescription =
            new Description<>(ChildType.CHILD, MoConditionalExpression.class, MoExpression.class,
                    "expression", true);

    private final static Description<MoConditionalExpression, MoExpression> thenExpressionDescription =
            new Description<>(ChildType.CHILD, MoConditionalExpression.class, MoExpression.class,
                    "thenExpression", true);

    private final static Description<MoConditionalExpression, MoExpression> elseExpressionDescription =
            new Description<>(ChildType.CHILD, MoConditionalExpression.class, MoExpression.class,
                    "elseExpression", true);

    private final static Map<String, Description<MoConditionalExpression, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("expression", conditionDescription),
            Map.entry("thenExpression", thenExpressionDescription),
            Map.entry("elseExpression", elseExpressionDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "expression", mandatory = true)
    private MoExpression condition;
    @RoleDescriptor(type = ChildType.CHILD, role = "thenExpression", mandatory = true)
    private MoExpression thenExpression;
    @RoleDescriptor(type = ChildType.CHILD, role = "elseExpression", mandatory = true)
    private MoExpression elseExpression;

    public MoConditionalExpression(Path fileName, int startLine, int endLine, ConditionalExpression conditionalExpression) {
        super(fileName, startLine, endLine, conditionalExpression);
        moNodeType = MoNodeType.TYPEConditionalExpression;
    }

    public void setCondition(MoExpression condition) {
        this.condition = condition;
    }

    public void setThenExpression(MoExpression thenExpression) {
        this.thenExpression = thenExpression;
    }

    public void setElseExpression(MoExpression elseExpression) {
        this.elseExpression = elseExpression;
    }

    public MoExpression getCondition() {
        return condition;
    }

    public MoExpression getThenExpression() {
        return thenExpression;
    }

    public MoExpression getElseExpression() {
        return elseExpression;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoConditionalExpression(this);
    }

    @Override
    public List<MoNode> getChildren() {
        return List.of(condition, thenExpression, elseExpression);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoConditionalExpression, ?> description = descriptionsMap.get(role);
        if (description == conditionDescription) {
            return condition;
        } else if (description == thenExpressionDescription) {
            return thenExpression;
        } else if (description == elseExpressionDescription) {
            return elseExpression;
        } else {
            logger.error("Role {} not found in MoConditionalExpression", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoConditionalExpression, ?> description = descriptionsMap.get(role);
        if (description == conditionDescription) {
            this.condition = (MoExpression) value;
        } else if (description == thenExpressionDescription) {
            this.thenExpression = (MoExpression) value;
        } else if (description == elseExpressionDescription) {
            this.elseExpression = (MoExpression) value;
        } else {
            logger.error("Role {} not found in MoConditionalExpression", role);
            return;
        }
    }

    public static Map<String, Description<MoConditionalExpression, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoConditionalExpression(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if (other instanceof MoConditionalExpression conditionalExpression) {
            return condition.isSame(conditionalExpression.condition) &&
                    thenExpression.isSame(conditionalExpression.thenExpression) &&
                    elseExpression.isSame(conditionalExpression.elseExpression);
        }
        return false;
    }
}
