package repair.ast.code.type;

import org.eclipse.jdt.core.dom.UnionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.statement.MoVariableDeclarationStatement;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MoUnionType extends MoType {
    private static final Logger logger = LoggerFactory.getLogger(MoUnionType.class);
    @Serial
    private static final long serialVersionUID = 727223199450938374L;

    private final static Description<MoUnionType, MoType> typesDescription =
            new Description<>(ChildType.CHILDLIST, MoUnionType.class, MoType.class,
                    "types", true);

    private final static Map<String, Description<MoUnionType, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("types", typesDescription)
    );

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "types", mandatory = true)
    private final MoNodeList<MoType> types;

    public MoUnionType(Path fileName, int startLine, int endLine, UnionType unionType) {
        super(fileName, startLine, endLine, unionType);
        moNodeType = MoNodeType.TYPEUnionType;
        types = new MoNodeList<>(this, typesDescription);
    }

    public void addType(MoType type) {
        types.add(type);
    }

    public List<MoType> getTypes() {
        return types;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoUnionType(this);
    }

    @Override
    public List<MoNode> getChildren() {
        return Collections.unmodifiableList(types);
    }

    @Override
    public boolean isLeaf() {
        return types.isEmpty();
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoUnionType, ?> description = descriptionsMap.get(role);
        if(description == typesDescription) {
            return types;
        } else {
            logger.error("Role {} not found in MoUnionType", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoUnionType, ?> description = descriptionsMap.get(role);
        if(description == typesDescription) {
            types.clear();
            types.addAll((List<MoType>) value);
        } else {
            logger.error("Role {} not found in MoUnionType", role);
        }
    }

    public static Map<String, Description<MoUnionType, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoUnionType(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if (other instanceof MoUnionType otherUnionType) {
            return MoNodeList.sameList(types, otherUnionType.types);
        }
        return false;
    }
}
