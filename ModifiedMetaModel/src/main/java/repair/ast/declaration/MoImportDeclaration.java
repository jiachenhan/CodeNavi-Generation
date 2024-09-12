package repair.ast.declaration;

import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoName;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.Map;

public class MoImportDeclaration extends MoNode {
    private static final Logger logger = LoggerFactory.getLogger(MoImportDeclaration.class);
    @Serial
    private static final long serialVersionUID = 7474358880727641490L;

    private final static Description<MoImportDeclaration, MoName> nameDescription =
            new Description<>(ChildType.CHILDLIST, MoImportDeclaration.class, MoName.class,
                    "name", true);

    private final static Description<MoImportDeclaration, Boolean> staticDescription =
            new Description<>(ChildType.SIMPLE, MoImportDeclaration.class, Boolean.class,
                    "static", true);

    private final static Description<MoImportDeclaration, Boolean> onDemandDescription =
            new Description<>(ChildType.SIMPLE, MoImportDeclaration.class, Boolean.class,
                    "onDemand", true);

    private final static Map<String, Description<MoImportDeclaration, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("name", nameDescription),
            Map.entry("static", staticDescription),
            Map.entry("onDemand", onDemandDescription)
    );


    @RoleDescriptor(type = ChildType.CHILD, role = "name", mandatory = true)
    private MoName name;
    @RoleDescriptor(type = ChildType.SIMPLE, role = "static", mandatory = true)
    private boolean isStatic;

    // isOnDemand = true
    // means import is like import java.util.*
    @RoleDescriptor(type = ChildType.SIMPLE, role = "onDemand", mandatory = true)
    private boolean isOnDemand;

    public MoImportDeclaration(String fileName, int startLine, int endLine, ImportDeclaration importDeclaration) {
        super(fileName, startLine, endLine, importDeclaration);
        moNodeType = MoNodeType.TYPEImportDeclaration;
    }

    public void setName(MoName name) {
        this.name = name;
    }

    public MoName getName() {
        return name;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isOnDemand() {
        return isOnDemand;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoImportDeclaration(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoImportDeclaration, ?> description = descriptionsMap.get(role);
        if(description == nameDescription) {
            return name;
        } else if(description == staticDescription) {
            return isStatic;
        } else if(description == onDemandDescription) {
            return isOnDemand;
        } else {
            logger.error("Role {} not found in MoImportDeclaration", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoImportDeclaration, ?> description = descriptionsMap.get(role);
        if(description == nameDescription) {
            name = (MoName) value;
        } else if(description == staticDescription) {
            isStatic = (boolean) value;
        } else if(description == onDemandDescription) {
            isOnDemand = (boolean) value;
        } else {
            logger.error("Role {} not found in MoImportDeclaration", role);
        }
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoImportDeclaration clone = new MoImportDeclaration(getFileName(), getStartLine(), getEndLine(), null);
        clone.setStructuralProperty("static", isStatic());
        clone.setStructuralProperty("onDemand", isOnDemand());
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoImportDeclaration otherImportDeclaration) {
            return name.isSame(otherImportDeclaration.name) &&
                    isStatic == otherImportDeclaration.isStatic &&
                    isOnDemand == otherImportDeclaration.isOnDemand;
        }
        return false;
    }
}
