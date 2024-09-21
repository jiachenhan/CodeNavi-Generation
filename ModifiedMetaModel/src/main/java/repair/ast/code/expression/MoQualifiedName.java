package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.QualifiedName;
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
import repair.ast.visitor.CodePrinter;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Map;

/**
 * 不再将其子节点展开，而是直接作为整体考虑identifier
 */
public class MoQualifiedName extends MoName {
    private static final Logger logger = LoggerFactory.getLogger(MoQualifiedName.class);
    @Serial
    private static final long serialVersionUID = -5918290711189275469L;

    private final static Description<MoQualifiedName, MoName> qualifierDescription =
            new Description<>(ChildType.CHILD, MoQualifiedName.class, MoName.class,
                    "qualifier", true);

    private final static Description<MoQualifiedName, MoSimpleName> nameDescription =
            new Description<>(ChildType.CHILD, MoQualifiedName.class, MoSimpleName.class,
                    "name", true);

    private final static Map<String, Description<MoQualifiedName, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("qualifier", qualifierDescription),
            Map.entry("name", nameDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "qualifier", mandatory = true)
    private MoName qualifier;
    @RoleDescriptor(type = ChildType.CHILD, role = "name", mandatory = true)
    private MoSimpleName name;



    public MoQualifiedName(String fileName, int startLine, int endLine, QualifiedName qualifiedName) {
        super(fileName, startLine, endLine, qualifiedName);
        moNodeType = MoNodeType.TYPEQualifiedName;
    }

    public void setQualifier(MoName qualifier) {
        this.qualifier = qualifier;
    }

    public void setName(MoSimpleName name) {
        this.name = name;
    }

    public MoName getQualifier() {
        return qualifier;
    }

    public MoSimpleName getName() {
        return name;
    }


    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoQualifiedName(this);
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
        Description<MoQualifiedName, ?> description = descriptionsMap.get(role);
        if(description == qualifierDescription) {
            return qualifier;
        } else if(description == nameDescription) {
            return name;
        } else {
            logger.error("Role {} not found in MoQualifiedName", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoQualifiedName, ?> description = descriptionsMap.get(role);
        if(description == qualifierDescription) {
            this.qualifier = (MoName) value;
        } else if(description == nameDescription) {
            this.name = (MoSimpleName) value;
        } else {
            logger.error("Role {} not found in MoQualifiedName", role);
        }
    }

    public static Map<String, Description<MoQualifiedName, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }



    @Override
    public MoNode shallowClone() {
        MoQualifiedName moQualifiedName = new MoQualifiedName(getFileName(), getStartLine(), getEndLine(), null);
        moQualifiedName.setIdentifier(toSrcString());
        return moQualifiedName;
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoQualifiedName otherQualifiedName) {
            return toSrcString().equals(otherQualifiedName.toSrcString());
        }
        return false;
    }

    /*
    * 在apply中，对于QualifiedName是按照整体进行匹配的，因此修改时只能直接修改identifier
    * */

    private String identifier;
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public String toSrcString() {
        if(identifier != null) {
            // 说明直接被修改了
            return identifier;
        }
        CodePrinter codePrinter = new CodePrinter();
        codePrinter.scan(this);
        identifier = codePrinter.getCode();
        return identifier;
    }

}
