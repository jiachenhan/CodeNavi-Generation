package repair.ast.code.statement;

import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoExpression;
import repair.ast.declaration.MoAbstractTypeDeclaration;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Map;

public class MoTypeDeclarationStatement extends MoStatement {
    private static final Logger logger = LoggerFactory.getLogger(MoTypeDeclarationStatement.class);
    @Serial
    private static final long serialVersionUID = 7844386872068622280L;
    private final static Description<MoTypeDeclarationStatement, MoAbstractTypeDeclaration> declarationDescription =
            new Description<>(ChildType.CHILD, MoTypeDeclarationStatement.class, MoAbstractTypeDeclaration.class,
                    "declaration", true);

    private final static Map<String, Description<MoTypeDeclarationStatement, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("declaration", declarationDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "declaration", mandatory = true)
    private MoAbstractTypeDeclaration typeDeclaration;

    public MoTypeDeclarationStatement(String fileName, int startLine, int endLine, TypeDeclarationStatement typeDeclarationStatement) {
        super(fileName, startLine, endLine, typeDeclarationStatement);
        moNodeType = MoNodeType.TYPETypeDeclarationStatement;
    }

    public void setTypeDeclaration(MoAbstractTypeDeclaration typeDeclaration) {
        this.typeDeclaration = typeDeclaration;
    }

    public MoAbstractTypeDeclaration getTypeDeclaration() {
        return typeDeclaration;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoTypeDeclarationStatement(this);
    }

    @Override
    public List<MoNode> getChildren() {
        return List.of(typeDeclaration);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoTypeDeclarationStatement, ?> description = descriptionsMap.get(role);
        if (description == declarationDescription) {
            return typeDeclaration;
        } else {
            logger.error("Role {} not found in MoExpressionStatement", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoTypeDeclarationStatement, ?> description = descriptionsMap.get(role);
        if (description == declarationDescription) {
            typeDeclaration = (MoAbstractTypeDeclaration) value;
        } else {
            logger.error("Role {} not found in MoExpressionStatement", role);
        }
    }

    public static Description<MoTypeDeclarationStatement, MoAbstractTypeDeclaration> getDeclarationDescription() {
        return declarationDescription;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoTypeDeclarationStatement(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if (other instanceof MoTypeDeclarationStatement otherTypeDeclarationStatement) {
            return typeDeclaration.isSame(otherTypeDeclarationStatement.typeDeclaration);
        }
        return false;
    }
}
