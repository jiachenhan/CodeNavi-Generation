package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.type.MoType;
import repair.ast.declaration.MoFieldDeclaration;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoSuperFieldAccess extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoSuperFieldAccess.class);
    @Serial
    private static final long serialVersionUID = 543288317466240146L;

    private final static Description<MoSuperFieldAccess, MoName> qualifierDescription =
            new Description<>(ChildType.CHILD, MoSuperFieldAccess.class, MoName.class,
                    "qualifier", false);

    private final static Description<MoSuperFieldAccess, MoSimpleName> nameDescription =
            new Description<>(ChildType.CHILD, MoSuperFieldAccess.class, MoSimpleName.class,
                    "name", true);

    private final static Map<String, Description<MoSuperFieldAccess, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("qualifier", qualifierDescription),
            Map.entry("name", nameDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "qualifier", mandatory = false)
    private MoName qualifier;
    @RoleDescriptor(type = ChildType.CHILD, role = "name", mandatory = true)
    private MoSimpleName name;

    public MoSuperFieldAccess(String fileName, int startLine, int endLine, SuperFieldAccess superFieldAccess) {
        super(fileName, startLine, endLine, superFieldAccess);
        moNodeType = MoNodeType.TYPESuperFieldAccess;
    }

    public void setQualifier(MoName qualifier) {
        this.qualifier = qualifier;
    }

    public void setName(MoSimpleName name) {
        this.name = name;
    }

    public Optional<MoName> getQualifier() {
        return Optional.ofNullable(qualifier);
    }

    public MoSimpleName getName() {
        return name;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoSuperFieldAccess(this);
    }

    @Override
    public List<MoNode> getChildren() {
        if(qualifier != null) {
            return List.of(qualifier, name);
        }
        return List.of(name);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoSuperFieldAccess, ?> description = descriptionsMap.get(role);
        if(description == qualifierDescription) {
            return qualifier;
        } else if(description == nameDescription) {
            return name;
        } else {
            logger.error("Role {} not found in MoSuperFieldAccess", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoSuperFieldAccess, ?> description = descriptionsMap.get(role);
        if(description == qualifierDescription) {
            this.qualifier = (MoName) value;
        } else if(description == nameDescription) {
            this.name = (MoSimpleName) value;
        } else {
            logger.error("Role {} not found in MoSuperFieldAccess", role);
        }
    }

    public static Map<String, Description<MoSuperFieldAccess, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoSuperFieldAccess(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoSuperFieldAccess moSuperFieldAccess) {
            boolean match = name.isSame(moSuperFieldAccess.name);
            if(qualifier == null) {
                match = match && moSuperFieldAccess.qualifier == null;
            } else {
                match = match && qualifier.isSame(moSuperFieldAccess.qualifier);
            }
            return match;
        }
        return false;
    }
}
