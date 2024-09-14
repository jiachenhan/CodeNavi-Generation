package repair.ast.declaration;

import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MoAnonymousClassDeclaration extends MoNode {
    private static final Logger logger = LoggerFactory.getLogger(MoAnonymousClassDeclaration.class);
    @Serial
    private static final long serialVersionUID = -7103561214841660714L;

    private final static Description<MoAnonymousClassDeclaration, MoBodyDeclaration> bodyDeclarationsDescription =
            new Description<>(ChildType.CHILDLIST, MoAnonymousClassDeclaration.class, MoBodyDeclaration.class,
                    "bodyDeclarations", true);

    private final static Map<String, Description<MoAnonymousClassDeclaration, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("bodyDeclarations", bodyDeclarationsDescription)
    );

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "bodyDeclarations", mandatory = true)
    private final MoNodeList<MoBodyDeclaration> bodyDeclarations;

    public MoAnonymousClassDeclaration(String fileName, int startLine, int endLine, AnonymousClassDeclaration anonymousClassDeclaration) {
        super(fileName, startLine, endLine, anonymousClassDeclaration);
        moNodeType = MoNodeType.TYPEAnonymousClassDeclaration;
        bodyDeclarations = new MoNodeList<>(this, bodyDeclarationsDescription);
    }

    public void addBodyDeclaration(MoBodyDeclaration bodyDeclaration) {
        bodyDeclarations.add(bodyDeclaration);
    }

    public List<MoBodyDeclaration> getBodyDeclarations() {
        return bodyDeclarations;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoAnonymousClassDeclaration(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoAnonymousClassDeclaration, ?> description = descriptionsMap.get(role);
        if(description == bodyDeclarationsDescription) {
            return bodyDeclarations;
        } else {
            logger.error("Role {} not found in MoAnonymousClassDeclaration", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoAnonymousClassDeclaration, ?> description = descriptionsMap.get(role);
        if(description == bodyDeclarationsDescription) {
            bodyDeclarations.clear();
            bodyDeclarations.addAll((List<MoBodyDeclaration>) value);
        } else {
            logger.error("Role {} not found in MoAnonymousClassDeclaration", role);
        }
    }

    public Map<String, Description<MoAnonymousClassDeclaration, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }


    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoAnonymousClassDeclaration(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoAnonymousClassDeclaration declaration){
            return MoNodeList.sameList(bodyDeclarations, declaration.bodyDeclarations);
        }
        return false;
    }
}
