package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.SimpleName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Map;

public class MoSimpleName extends MoName {
    private static final Logger logger = LoggerFactory.getLogger(MoSimpleName.class);
    @Serial
    private static final long serialVersionUID = -7733131160320479651L;

    private final static Description<MoSimpleName, String> identifierDescription =
            new Description<>(ChildType.SIMPLE, MoSimpleName.class, String.class,
                    "identifier", true);

    private final static Map<String, Description<MoSimpleName, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("identifier", identifierDescription)
    );

    @RoleDescriptor(type = ChildType.SIMPLE, role = "identifier", mandatory = true)
    private String identifier;

    public MoSimpleName(String fileName, int startLine, int endLine, SimpleName simpleName) {
        super(fileName, startLine, endLine, simpleName);
        moNodeType = MoNodeType.TYPESimpleName;
    }

    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoSimpleName(this);
    }

    @Override
    public List<MoNode> getChildren() {
        return List.of();
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoSimpleName, ?> description = descriptionsMap.get(role);
        if(description == identifierDescription) {
            return identifier;
        } else {
            logger.error("Role {} not found in MoSimpleName", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoSimpleName, ?> description = descriptionsMap.get(role);
        if(description == identifierDescription) {
            identifier = (String) value;
        } else {
            logger.error("Role {} not found in MoSimpleName", role);
        }
    }

    public static Map<String, Description<MoSimpleName, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoSimpleName clone = new MoSimpleName(getFileName(), getStartLine(), getEndLine(), null);
        clone.setStructuralProperty("identifier", getIdentifier());
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoSimpleName moSimpleName) {
            return identifier.equals(moSimpleName.identifier);
        }
        return false;
    }
}
