package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoCompilationUnit;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoExpression;
import repair.ast.declaration.MoPackageDeclaration;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class MoDoStatement extends MoStatement {
    private static final Logger logger = LoggerFactory.getLogger(MoDoStatement.class);
    @Serial
    private static final long serialVersionUID = 115794184620714246L;

    private final static Description<MoDoStatement, MoStatement> bodyDescription =
            new Description<>(ChildType.CHILD, MoDoStatement.class, MoStatement.class,
                    "body", true);

    private final static Description<MoDoStatement, MoExpression> expressionDescription =
            new Description<>(ChildType.CHILD, MoDoStatement.class, MoExpression.class,
                    "expression", true);

    private final static Map<String, Description<MoDoStatement, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("body", bodyDescription),
            Map.entry("expression", expressionDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "body", mandatory = true)
    private MoStatement body;

    @RoleDescriptor(type = ChildType.CHILD, role = "expression", mandatory = true)
    private MoExpression expression;

    public MoDoStatement(Path fileName, int startLine, int endLine, DoStatement doStatement) {
        super(fileName, startLine, endLine, doStatement);
        moNodeType = MoNodeType.TYPEDoStatement;
    }

    public void setBody(MoStatement body) {
        this.body = body;
    }

    public void setExpression(MoExpression expression) {
        this.expression = expression;
    }


    public MoStatement getBody() {
        return body;
    }

    public MoExpression getExpression() {
        return expression;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoDoStatement(this);
    }

    @Override
    public List<MoNode> getChildren() {
        return List.of(body, expression);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoDoStatement, ?> description = descriptionsMap.get(role);
        if(description == bodyDescription) {
            return body;
        } else if(description == expressionDescription) {
            return expression;
        } else {
            logger.error("Role {} not found in MoDoStatement", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoDoStatement, ?> description = descriptionsMap.get(role);
        if(description == bodyDescription) {
            this.body = (MoStatement) value;
        } else if(description == expressionDescription) {
            this.expression = (MoExpression) value;
        } else {
            logger.error("Role {} not found in MoDoStatement", role);
        }
    }

    public static Map<String, Description<MoDoStatement, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoDoStatement(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoDoStatement otherDoStatement) {
            return body.isSame(otherDoStatement.body) &&
                    expression.isSame(otherDoStatement.expression);
        }
        return false;
    }
}
