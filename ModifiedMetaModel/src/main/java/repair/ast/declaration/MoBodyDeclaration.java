package repair.ast.declaration;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.code.MoExtendedModifier;
import repair.ast.code.MoJavadoc;
import repair.ast.role.ChildType;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Optional;

public abstract class MoBodyDeclaration extends MoNode {
    @Serial
    private static final long serialVersionUID = -6632920743935269248L;

    @RoleDescriptor(type = ChildType.CHILD, role = "javadoc", mandatory = false)
    protected MoJavadoc javadoc;

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "modifiers", mandatory = true)
    protected MoNodeList<MoExtendedModifier> modifiers;

    public MoBodyDeclaration(String fileName, int startLine, int endLine, BodyDeclaration bodyDeclaration) {
        super(fileName, startLine, endLine, bodyDeclaration);
    }

    public Optional<MoJavadoc> getJavadoc() {
        return Optional.ofNullable(javadoc);
    }

    public List<MoExtendedModifier> getModifiers() {
        return modifiers;
    }

    public void setJavadoc(MoJavadoc javadoc) {
        this.javadoc = javadoc;
    }

    public void addModifier(MoExtendedModifier modifier) {
        modifiers.add(modifier);
    }
}
