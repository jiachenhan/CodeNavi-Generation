package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
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

public class MoSynchronizedStatement extends MoStatement {
    private static final Logger logger = LoggerFactory.getLogger(MoSynchronizedStatement.class);
    @Serial
    private static final long serialVersionUID = -3252276150836821128L;
    private final static Description<MoSynchronizedStatement, MoExpression> expressionDescription =
            new Description<>(ChildType.CHILD, MoSynchronizedStatement.class, MoExpression.class,
                    "expression", true);

    private final static Description<MoSynchronizedStatement, MoBlock> bodyDescription =
            new Description<>(ChildType.CHILD, MoSynchronizedStatement.class, MoBlock.class,
                    "body", true);

    private final static Map<String, Description<MoSynchronizedStatement, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("expression", expressionDescription),
            Map.entry("body", bodyDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "expression", mandatory = true)
    private MoExpression expression;
    @RoleDescriptor(type = ChildType.CHILD, role = "body", mandatory = true)
    private MoBlock block;

    public MoSynchronizedStatement(String fileName, int startLine, int endLine, SynchronizedStatement synchronizedStatement) {
        super(fileName, startLine, endLine, synchronizedStatement);
        moNodeType = MoNodeType.TYPESynchronizedStatement;
    }


    public void setExpression(MoExpression expression) {
        this.expression = expression;
    }

    public void setBlock(MoBlock block) {
        this.block = block;
    }

    public MoExpression getExpression() {
        return expression;
    }

    public MoBlock getBlock() {
        return block;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoSynchronizedStatement(this);
    }

    @Override
    public List<MoNode> getChildren() {
        return List.of(expression, block);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoSynchronizedStatement, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            return expression;
        } else if(description == bodyDescription) {
            return block;
        } else {
            logger.error("Role {} not found in MoSynchronizedStatement", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoSynchronizedStatement, ?> description = descriptionsMap.get(role);
        if(description == expressionDescription) {
            this.expression = (MoExpression) value;
        } else if(description == bodyDescription) {
            this.block = (MoBlock) value;
        } else {
            logger.error("Role {} not found in MoSynchronizedStatement", role);
        }
    }

    public static Map<String, Description<MoSynchronizedStatement, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoSynchronizedStatement(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoSynchronizedStatement moSynchronizedStatement) {
            return expression.isSame(moSynchronizedStatement.expression) &&
                    block.isSame(moSynchronizedStatement.block);
        }
        return false;
    }
}
