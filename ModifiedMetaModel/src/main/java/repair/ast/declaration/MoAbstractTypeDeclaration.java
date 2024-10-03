package repair.ast.declaration;

import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.code.MoExtendedModifier;
import repair.ast.code.MoJavadoc;
import repair.ast.code.expression.MoSimpleName;
import repair.ast.role.ChildType;
import repair.ast.role.RoleDescriptor;

import java.io.Serial;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class MoAbstractTypeDeclaration extends MoBodyDeclaration {
    @Serial
    private static final long serialVersionUID = 3169995312754341299L;

    @RoleDescriptor(type = ChildType.CHILD, role = "name", mandatory = true)
    protected MoSimpleName name;

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "bodyDeclarations", mandatory = true)
    protected MoNodeList<MoBodyDeclaration> bodyDeclarations;

    public MoAbstractTypeDeclaration(Path fileName, int startLine, int endLine, AbstractTypeDeclaration abstractTypeDeclaration) {
        super(fileName, startLine, endLine, abstractTypeDeclaration);
    }

    public void setName(MoSimpleName name) {
        this.name = name;
    }

    public void addBodyDeclaration(MoBodyDeclaration moBodyDeclaration) {
        bodyDeclarations.add(moBodyDeclaration);
    }

    public MoSimpleName getName() {
        return name;
    }

    public List<MoBodyDeclaration> getBodyDeclarations() {
        return bodyDeclarations;
    }

}
