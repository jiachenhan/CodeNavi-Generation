package repair.ast.code.type;

import org.eclipse.jdt.core.dom.SimpleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.MoExtendedModifier;
import repair.ast.code.expression.MoAnnotation;
import repair.ast.code.expression.MoName;
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

public class MoSimpleType extends MoAnnotatableType {
    private static final Logger logger = LoggerFactory.getLogger(MoSimpleType.class);
    @Serial
    private static final long serialVersionUID = -7632221698611021798L;

    private final static Description<MoSimpleType, MoAnnotation> annotationsDescription =
            new Description<>(ChildType.CHILDLIST, MoSimpleType.class, MoAnnotation.class,
                    "annotations", true);

    private final static Description<MoSimpleType, MoName> nameDescription =
            new Description<>(ChildType.CHILD, MoSimpleType.class, MoName.class,
                    "name", true);

    private final static Map<String, Description<MoSimpleType, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("annotations", annotationsDescription),
            Map.entry("name", nameDescription)
    );


    @RoleDescriptor(type = ChildType.CHILD, role = "name", mandatory = true)
    private MoName name;

    public MoSimpleType(Path fileName, int startLine, int endLine, SimpleType simpleType) {
        super(fileName, startLine, endLine, simpleType);
        moNodeType = MoNodeType.TYPESimpleType;
        super.annotations = new MoNodeList<>(this, annotationsDescription);
    }

    public void setName(MoName name) {
        this.name = name;
    }
    public MoName getName() {
        return name;
    }
    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoSimpleType(this);
    }

    @Override
    public List<MoNode> getChildren() {
        List<MoNode> children = new ArrayList<>(annotations);
        children.add(name);
        return Collections.unmodifiableList(children);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoSimpleType, ?> description = descriptionsMap.get(role);
        if(description == annotationsDescription) {
            return super.annotations;
        } else if(description == nameDescription) {
            return name;
        } else {
            logger.error("Role {} not found in MoSimpleType", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoSimpleType, ?> description = descriptionsMap.get(role);
        if(description == nameDescription) {
            this.name = (MoName) value;
        } else {
            logger.error("Role {} not found in MoSimpleType", role);
        }
    }

    public static Map<String, Description<MoSimpleType, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoSimpleType(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoSimpleType otherSimpleType) {
            boolean match = MoExtendedModifier.sameList(this.annotations, otherSimpleType.annotations);
            match = match && this.name.isSame(otherSimpleType.name);
            return match;
        }
        return false;
    }
}
