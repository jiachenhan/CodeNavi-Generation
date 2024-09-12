package repair.ast.declaration;

import org.eclipse.jdt.core.dom.VariableDeclaration;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.code.MoDimension;
import repair.ast.code.expression.MoExpression;
import repair.ast.code.expression.MoSimpleName;
import repair.ast.role.ChildType;
import repair.ast.role.RoleDescriptor;

import java.io.Serial;
import java.util.List;
import java.util.Optional;

public abstract class MoVariableDeclaration extends MoNode {
    @Serial
    private static final long serialVersionUID = 8116633790504454147L;
    @RoleDescriptor(type = ChildType.CHILD, role = "name", mandatory = true)
    protected MoSimpleName name;
    @RoleDescriptor(type = ChildType.CHILDLIST, role = "extraDimensions2", mandatory = true)
    protected MoNodeList<MoDimension> CStyleArrayDimensions;
    @RoleDescriptor(type = ChildType.CHILD, role = "initializer", mandatory = false)
    protected MoExpression initializer;

    public MoVariableDeclaration(String fileName, int startLine, int endLine, VariableDeclaration variableDeclaration) {
        super(fileName, startLine, endLine, variableDeclaration);
    }

    public void setName(MoSimpleName name) {
        this.name = name;
    }

    public void addCStyleArrayDimension(MoDimension dimension) {
        CStyleArrayDimensions.add(dimension);
    }

    public void setInitializer(MoExpression initializer) {
        this.initializer = initializer;
    }

    public MoSimpleName getName() {
        return name;
    }

    public List<MoDimension> getCStyleArrayDimensions() {
        return CStyleArrayDimensions;
    }

    public Optional<MoExpression> getInitializer() {
        return Optional.ofNullable(initializer);
    }
}
