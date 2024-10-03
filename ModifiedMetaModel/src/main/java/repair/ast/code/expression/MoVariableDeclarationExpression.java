package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.MoExtendedModifier;
import repair.ast.code.type.MoType;
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

public class MoVariableDeclarationExpression extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoVariableDeclarationExpression.class);
    @Serial
    private static final long serialVersionUID = -8161542718412240172L;

    private final static Description<MoVariableDeclarationExpression, MoExtendedModifier> modifiersDescription =
            new Description<>(ChildType.CHILDLIST, MoVariableDeclarationExpression.class, MoExtendedModifier.class,
                    "modifiers", true);

    private final static Description<MoVariableDeclarationExpression, MoType> typeDescription =
            new Description<>(ChildType.CHILD, MoVariableDeclarationExpression.class, MoType.class,
                    "type", true);

    private final static Description<MoVariableDeclarationExpression, MoVariableDeclarationFragment> fragmentsDescription =
            new Description<>(ChildType.CHILDLIST, MoVariableDeclarationExpression.class, MoVariableDeclarationFragment.class,
                    "fragments", true);

    private final static Map<String, Description<MoVariableDeclarationExpression, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("modifiers", modifiersDescription),
            Map.entry("type", typeDescription),
            Map.entry("fragments", fragmentsDescription)
    );

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "modifiers", mandatory = true)
    private final MoNodeList<MoExtendedModifier> modifiers;
    @RoleDescriptor(type = ChildType.CHILD, role = "type", mandatory = true)
    private MoType type;
    @RoleDescriptor(type = ChildType.CHILDLIST, role = "fragments", mandatory = true)
    private final MoNodeList<MoVariableDeclarationFragment> fragments;


    public MoVariableDeclarationExpression(Path fileName, int startLine, int endLine, VariableDeclarationExpression variableDeclarationExpression) {
        super(fileName, startLine, endLine, variableDeclarationExpression);
        moNodeType = MoNodeType.TYPEVariableDeclarationExpression;
        modifiers = new MoNodeList<>(this, modifiersDescription);
        fragments = new MoNodeList<>(this, fragmentsDescription);
    }

    public void addModifier(MoExtendedModifier modifier) {
        modifiers.add(modifier);
    }

    public void setType(MoType type) {
        this.type = type;
    }

    public void addFragment(MoVariableDeclarationFragment fragment) {
        fragments.add(fragment);
    }

    public List<MoExtendedModifier> getModifiers() {
        return modifiers;
    }

    public MoType getType() {
        return type;
    }

    public List<MoVariableDeclarationFragment> getFragments() {
        return fragments;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoVariableDeclarationExpression(this);
    }

    @Override
    public List<MoNode> getChildren() {
        List<MoNode> children = new ArrayList<>();
        modifiers.forEach(modifier -> children.add((MoNode) modifier));
        children.add(type);
        children.addAll(fragments);
        return Collections.unmodifiableList(children);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoVariableDeclarationExpression, ?> description = descriptionsMap.get(role);
        if(description == modifiersDescription) {
            return modifiers;
        } else if(description == typeDescription) {
            return type;
        } else if(description == fragmentsDescription) {
            return fragments;
        } else {
            logger.error("Role {} not found in MoVariableDeclarationExpression", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoVariableDeclarationExpression, ?> description = descriptionsMap.get(role);
        if(description == modifiersDescription) {
            modifiers.clear();
            modifiers.addAll((List<MoExtendedModifier>) value);
        } else if(description == typeDescription) {
            type = (MoType) value;
        } else if(description == fragmentsDescription) {
            fragments.clear();
            fragments.addAll((List<MoVariableDeclarationFragment>) value);
        } else {
            logger.error("Role {} not found in MoVariableDeclarationExpression", role);
        }
    }

    public static Map<String, Description<MoVariableDeclarationExpression, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoVariableDeclarationExpression(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoVariableDeclarationExpression otherVariableDeclarationExpression) {
            boolean match = MoExtendedModifier.sameList(modifiers, otherVariableDeclarationExpression.modifiers);
            match = match && type.isSame(otherVariableDeclarationExpression.type);
            return match && MoNodeList.sameList(fragments, otherVariableDeclarationExpression.fragments);
        }
        return false;
    }
}
