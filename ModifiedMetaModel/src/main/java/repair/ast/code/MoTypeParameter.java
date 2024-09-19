package repair.ast.code;

import org.eclipse.jdt.core.dom.TypeParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoSimpleName;
import repair.ast.code.type.MoType;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * TypeParameter:
 *      { ExtendedModifier } Identifier [ extends Type { & Type } ]
 */
public class MoTypeParameter extends MoNode {
    private static final Logger logger = LoggerFactory.getLogger(MoTypeParameter.class);
    @Serial
    private static final long serialVersionUID = -5909190117968875089L;

    private final static Description<MoTypeParameter, MoExtendedModifier> modifiersDescription =
            new Description<>(ChildType.CHILDLIST, MoTypeParameter.class, MoExtendedModifier.class,
                    "modifiers", true);

    private final static Description<MoTypeParameter, MoSimpleName> nameDescription =
            new Description<>(ChildType.CHILD, MoTypeParameter.class, MoSimpleName.class,
                    "name", true);

    private final static Description<MoTypeParameter, MoType> typeBoundsDescription =
            new Description<>(ChildType.CHILDLIST, MoTypeParameter.class, MoType.class,
                    "typeBounds", true);

    private final static Map<String, Description<MoTypeParameter, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("modifiers", modifiersDescription),
            Map.entry("name", nameDescription),
            Map.entry("typeBounds", typeBoundsDescription)
    );

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "modifiers", mandatory = true)
    private final MoNodeList<MoExtendedModifier> modifiers;
    @RoleDescriptor(type = ChildType.CHILD, role = "name", mandatory = true)
    private MoSimpleName name;
    @RoleDescriptor(type = ChildType.CHILDLIST, role = "typeBounds", mandatory = true)
    private final MoNodeList<MoType> typeBounds;

    public MoTypeParameter(String fileName, int startLine, int endLine, TypeParameter typeParameter) {
        super(fileName, startLine, endLine, typeParameter);
        moNodeType = MoNodeType.TYPETypeParameter;
        modifiers = new MoNodeList<>(this, modifiersDescription);
        typeBounds = new MoNodeList<>(this, typeBoundsDescription);
    }

    public void addModifier(MoExtendedModifier modifier) {
        modifiers.add(modifier);
    }

    public void setName(MoSimpleName name) {
        this.name = name;
    }

    public void addTypeBound(MoType typeBound) {
        typeBounds.add(typeBound);
    }

    public List<MoExtendedModifier> getModifiers() {
        return modifiers;
    }

    public MoSimpleName getName() {
        return name;
    }

    public List<MoType> getTypeBounds() {
        return typeBounds;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoTypeParameter(this);
    }

    @Override
    public List<MoNode> getChildren() {
        List<MoNode> children = new ArrayList<>();
        modifiers.forEach(modifier -> children.add(((MoNode) modifier)));
        children.add(name);
        children.addAll(typeBounds);
        return Collections.unmodifiableList(children);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoTypeParameter, ?> description = descriptionsMap.get(role);
        if(description == modifiersDescription) {
            return modifiers;
        } else if(description == nameDescription) {
            return name;
        } else if(description == typeBoundsDescription) {
            return typeBounds;
        } else {
            logger.error("Role {} not found in MoTypeParameter", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoTypeParameter, ?> description = descriptionsMap.get(role);
        if(description == modifiersDescription) {
            modifiers.clear();
            modifiers.addAll((List<MoExtendedModifier>) value);
        } else if(description == nameDescription) {
            name = (MoSimpleName) value;
        } else if(description == typeBoundsDescription) {
            typeBounds.clear();
            typeBounds.addAll((List<MoType>) value);
        } else {
            logger.error("Role {} not found in MoTypeParameter", role);
        }
    }

    public static Map<String, Description<MoTypeParameter, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoTypeParameter(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoTypeParameter typeParameter){
            return MoExtendedModifier.sameList(modifiers, typeParameter.modifiers) &&
                    name.isSame(typeParameter.name) &&
                    MoNodeList.sameList(typeBounds, typeParameter.typeBounds);
        }
        return false;
    }
}
