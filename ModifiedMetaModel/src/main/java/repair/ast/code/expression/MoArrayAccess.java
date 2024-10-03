package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.ArrayAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.declaration.MoAnonymousClassDeclaration;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class MoArrayAccess extends MoExpression{
    private static final Logger logger = LoggerFactory.getLogger(MoArrayAccess.class);
    @Serial
    private static final long serialVersionUID = -9199386571438345578L;

    private final static Description<MoArrayAccess, MoExpression> arrayDescription =
            new Description<>(ChildType.CHILD, MoArrayAccess.class, MoExpression.class,
                    "array", true);

    private final static Description<MoArrayAccess, MoExpression> indexDescription =
            new Description<>(ChildType.CHILD, MoArrayAccess.class, MoExpression.class,
                    "index", true);

    private final static Map<String, Description<MoArrayAccess, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("array", arrayDescription),
            Map.entry("index", indexDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "array", mandatory = true)
    private MoExpression array;
    @RoleDescriptor(type = ChildType.CHILD, role = "index", mandatory = true)
    private MoExpression index;

    public MoArrayAccess(Path fileName, int startLine, int endLine, ArrayAccess arrayAccess) {
        super(fileName, startLine, endLine, arrayAccess);
        moNodeType = MoNodeType.TYPEArrayAccess;
    }

    public void setArray(MoExpression array) {
        this.array = array;
    }

    public void setIndex(MoExpression index) {
        this.index = index;
    }

    public MoExpression getArray() {
        return array;
    }

    public MoExpression getIndex() {
        return index;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoArrayAccess(this);
    }

    @Override
    public List<MoNode> getChildren() {
        return List.of(array, index);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoArrayAccess, ?> description = descriptionsMap.get(role);
        if(description == arrayDescription) {
            return array;
        } else if(description == indexDescription) {
            return index;
        } else {
            logger.error("Role {} not found in MoArrayAccess", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoArrayAccess, ?> description = descriptionsMap.get(role);
        if(description == arrayDescription) {
            setArray((MoExpression) value);
        } else if(description == indexDescription) {
            setIndex((MoExpression) value);
        } else {
            logger.error("Role {} not found in MoArrayAccess", role);
        }
    }

    public Map<String, Description<MoArrayAccess, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoArrayAccess(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoArrayAccess arrayAccess){
            return array.isSame(arrayAccess.array) && index.isSame(arrayAccess.index);
        }
        return false;
    }
}
