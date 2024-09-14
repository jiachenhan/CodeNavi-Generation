package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.MoCatchClause;
import repair.ast.code.MoExtendedModifier;
import repair.ast.code.MoJavadoc;
import repair.ast.code.expression.MoVariableDeclarationExpression;
import repair.ast.code.type.MoType;
import repair.ast.declaration.MoFieldDeclaration;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoTryStatement extends MoStatement {
    private static final Logger logger = LoggerFactory.getLogger(MoTryStatement.class);
    @Serial
    private static final long serialVersionUID = 1120218215905412870L;

    private final static Description<MoTryStatement, MoVariableDeclarationExpression> resourcesDescription =
            new Description<>(ChildType.CHILDLIST, MoTryStatement.class, MoVariableDeclarationExpression.class,
                    "resources", true);

    private final static Description<MoTryStatement, MoBlock> bodyDescription =
            new Description<>(ChildType.CHILD, MoTryStatement.class, MoBlock.class,
                    "body", true);

    private final static Description<MoTryStatement, MoCatchClause> catchClausesDescription =
            new Description<>(ChildType.CHILDLIST, MoTryStatement.class, MoCatchClause.class,
                    "catchClauses", true);

    private final static Description<MoTryStatement, MoBlock> finallyDescription =
            new Description<>(ChildType.CHILD, MoTryStatement.class, MoBlock.class,
                    "finally", false);

    private final static Map<String, Description<MoTryStatement, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("resources", resourcesDescription),
            Map.entry("body", bodyDescription),
            Map.entry("catchClauses", catchClausesDescription),
            Map.entry("finally", finallyDescription)
    );

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "resources", mandatory = true)
    private final MoNodeList<MoVariableDeclarationExpression> resources;
    @RoleDescriptor(type = ChildType.CHILD, role = "body", mandatory = true)
    private MoBlock tryBlock;
    @RoleDescriptor(type = ChildType.CHILDLIST, role = "catchClauses", mandatory = true)
    private final MoNodeList<MoCatchClause> catchClauses;
    @RoleDescriptor(type = ChildType.CHILD, role = "finally", mandatory = false)
    private MoBlock finallyBlock;

    public MoTryStatement(String fileName, int startLine, int endLine, TryStatement tryStatement) {
        super(fileName, startLine, endLine, tryStatement);
        moNodeType = MoNodeType.TYPETryStatement;
        resources = new MoNodeList<>(this, resourcesDescription);
        catchClauses = new MoNodeList<>(this, catchClausesDescription);
    }

    public void addResource(MoVariableDeclarationExpression resource) {
        resources.add(resource);
    }

    public void setTryBlock(MoBlock tryBlock) {
        this.tryBlock = tryBlock;
    }

    public void addCatchClause(MoCatchClause catchClause) {
        catchClauses.add(catchClause);
    }

    public void setFinallyBlock(MoBlock finallyBlock) {
        this.finallyBlock = finallyBlock;
    }

    public List<MoVariableDeclarationExpression> getResources() {
        return resources;
    }

    public MoBlock getTryBlock() {
        return tryBlock;
    }

    public List<MoCatchClause> getCatchClauses() {
        return catchClauses;
    }

    public Optional<MoBlock> getFinallyBlock() {
        return Optional.ofNullable(finallyBlock);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoTryStatement(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoTryStatement, ?> description = descriptionsMap.get(role);
        if(description == resourcesDescription) {
            return resources;
        } else if(description == bodyDescription) {
            return tryBlock;
        } else if(description == catchClausesDescription) {
            return catchClauses;
        } else if(description == finallyDescription) {
            return finallyBlock;
        } else {
            logger.error("Role {} not found in MoTryStatement", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoTryStatement, ?> description = descriptionsMap.get(role);
        if(description == resourcesDescription) {
            resources.clear();
            resources.addAll((List<MoVariableDeclarationExpression>) value);
        } else if(description == bodyDescription) {
            tryBlock = (MoBlock) value;
        } else if(description == catchClausesDescription) {
            catchClauses.clear();
            catchClauses.addAll((List<MoCatchClause>) value);
        } else if(description == finallyDescription) {
            finallyBlock = (MoBlock) value;
        } else {
            logger.error("Role {} not found in MoTryStatement", role);
        }
    }

    public static Map<String, Description<MoTryStatement, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoTryStatement(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoTryStatement moTryStatement) {
            boolean match = MoNodeList.sameList(resources, moTryStatement.resources);
            match = match && tryBlock.isSame(moTryStatement.tryBlock);
            match = match && MoNodeList.sameList(catchClauses, moTryStatement.catchClauses);
            if(finallyBlock == null) {
                return match && moTryStatement.finallyBlock == null;
            } else {
                return match && finallyBlock.isSame(moTryStatement.finallyBlock);
            }
        }
        return false;
    }
}
