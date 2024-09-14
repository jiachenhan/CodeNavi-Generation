package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.CreationReference;
import org.eclipse.jdt.core.dom.MethodReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.statement.MoVariableDeclarationStatement;
import repair.ast.code.type.MoType;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.Map;

/**
 * CreationReference:
 *       Type ::
 *           [ < Type { , Type } > ]
 *           new
 */
public class MoCreationReference extends MoMethodReference {
    private static final Logger logger = LoggerFactory.getLogger(MoCreationReference.class);
    @Serial
    private static final long serialVersionUID = -6293494195306708688L;

    private final static Description<MoCreationReference, MoType> typeArgumentsDescription =
            new Description<>(ChildType.CHILDLIST, MoCreationReference.class, MoType.class,
                    "typeArguments", true);

    private final static Description<MoCreationReference, MoType> typeDescription =
            new Description<>(ChildType.CHILD, MoCreationReference.class, MoType.class,
                    "type", true);

    private final static Map<String, Description<MoCreationReference, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("typeArguments", typeArgumentsDescription),
            Map.entry("type", typeDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "type", mandatory = true)
    private MoType type;

    public MoCreationReference(String fileName, int startLine, int endLine, CreationReference creationReference) {
        super(fileName, startLine, endLine, creationReference);
        moNodeType = MoNodeType.TYPECreationReference;
        super.typeArguments = new MoNodeList<>(this, typeArgumentsDescription);
    }

    public void setType(MoType type) {
        this.type = type;
    }

    public MoType getType() {
        return type;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoCreationReference(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoCreationReference, ?> description = descriptionsMap.get(role);
        if(description == typeArgumentsDescription) {
            return super.typeArguments;
        } else if(description == typeDescription) {
            return type;
        } else {
            logger.error("Role {} not found in MoCreationReference", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoCreationReference, ?> description = descriptionsMap.get(role);
        if(description == typeArgumentsDescription) {
            super.typeArguments = (MoNodeList<MoType>) value;
        } else if(description == typeDescription) {
            this.type = (MoType) value;
        } else {
            logger.error("Role {} not found in MoCreationReference", role);
        }
    }

    public static Map<String, Description<MoCreationReference, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoCreationReference(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoCreationReference otherCreationReference) {
            return MoNodeList.sameList(super.typeArguments, otherCreationReference.typeArguments)
                    && type.isSame(otherCreationReference.type);
        }
        return false;
    }
}
