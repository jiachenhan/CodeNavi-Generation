package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.declaration.MoAnonymousClassDeclaration;
import repair.ast.declaration.MoBodyDeclaration;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.nio.file.Path;
import java.util.*;


public class MoBlock extends MoStatement {
    private static final Logger logger = LoggerFactory.getLogger(MoBlock.class);
    @Serial
    private static final long serialVersionUID = -1917149794879212869L;

    private final static Description<MoBlock, MoStatement> statementsDescription =
            new Description<>(ChildType.CHILDLIST, MoBlock.class, MoStatement.class,
                    "statements", true);

    private final static Map<String, Description<MoBlock, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("statements", statementsDescription)
    );

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "statements",  mandatory = true)
    private final MoNodeList<MoStatement> statements;

    public MoBlock(Path fileName, int startLine, int endLine, Block block) {
        super(fileName, startLine, endLine, block);
        moNodeType = MoNodeType.TYPEBlock;
        statements = new MoNodeList<>(this, statementsDescription);
    }

    public List<MoStatement> getStatements() {
        return statements;
    }

    public void addStatement(MoStatement moStatement) {
        statements.add(moStatement);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoBlock(this);
    }

    @Override
    public List<MoNode> getChildren() {
        return Collections.unmodifiableList(statements);
    }

    @Override
    public boolean isLeaf() {
        return statements.isEmpty();
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoBlock, ?> description = descriptionsMap.get(role);
        if(description == statementsDescription) {
            return statements;
        } else {
            logger.error("Role {} not found in MoBlock", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoBlock, ?> description = descriptionsMap.get(role);
        if(description == statementsDescription) {
            statements.clear();
            statements.addAll((List<MoStatement>) value);
        } else {
            logger.error("Role {} not found in MoBlock", role);
        }
    }

    public static Map<String, Description<MoBlock, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoBlock(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoBlock moBlock) {
            return MoNodeList.sameList(statements, moBlock.statements);
        }
        return false;
    }
}
