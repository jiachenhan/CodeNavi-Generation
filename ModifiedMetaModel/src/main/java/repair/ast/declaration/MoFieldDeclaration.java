package repair.ast.declaration;

import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.MoExtendedModifier;
import repair.ast.code.MoJavadoc;
import repair.ast.code.type.MoType;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Map;

public class MoFieldDeclaration extends MoBodyDeclaration {
    private static final Logger logger = LoggerFactory.getLogger(MoFieldDeclaration.class);
    @Serial
    private static final long serialVersionUID = -4582683608053036584L;

    private final static Description<MoFieldDeclaration, MoJavadoc> javadocDescription =
            new Description<>(ChildType.CHILD, MoFieldDeclaration.class, MoJavadoc.class,
                    "javadoc", false);

    private final static Description<MoFieldDeclaration, MoExtendedModifier> modifiersDescription =
            new Description<>(ChildType.CHILDLIST, MoFieldDeclaration.class, MoExtendedModifier.class,
                    "modifiers", true);

    private final static Description<MoFieldDeclaration, MoType> typeDescription =
            new Description<>(ChildType.CHILD, MoFieldDeclaration.class, MoType.class,
                    "type", true);

    private final static Description<MoFieldDeclaration, MoVariableDeclarationFragment> fragmentsDescription =
            new Description<>(ChildType.CHILDLIST, MoFieldDeclaration.class, MoVariableDeclarationFragment.class,
                    "fragments", true);

    private final static Map<String, Description<MoFieldDeclaration, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("javadoc", javadocDescription),
            Map.entry("modifiers", modifiersDescription),
            Map.entry("type", typeDescription),
            Map.entry("fragments", fragmentsDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "type", mandatory = true)
    private MoType type;

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "fragments", mandatory = true)
    private final MoNodeList<MoVariableDeclarationFragment> fragments;

    public MoFieldDeclaration(String fileName, int startLine, int endLine, FieldDeclaration fieldDeclaration) {
        super(fileName, startLine, endLine, fieldDeclaration);
        moNodeType = MoNodeType.TYPEFieldDeclaration;
        super.modifiers = new MoNodeList<>(this, modifiersDescription);
        fragments = new MoNodeList<>(this, fragmentsDescription);
    }

    public void setType(MoType type) {
        this.type = type;
    }

    public void addFragment(MoVariableDeclarationFragment fragment) {
        fragments.add(fragment);
    }

    public MoType getType() {
        return type;
    }

    public List<MoVariableDeclarationFragment> getFragments() {
        return fragments;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoFieldDeclaration(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoFieldDeclaration, ?> description = descriptionsMap.get(role);
        if(description == javadocDescription) {
            return super.javadoc;
        } else if(description == modifiersDescription) {
            return super.modifiers;
        } else if(description == typeDescription) {
            return type;
        } else if(description == fragmentsDescription) {
            return fragments;
        } else {
            logger.error("Role {} not found in MoFieldDeclaration", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoFieldDeclaration, ?> description = descriptionsMap.get(role);
        if(description == javadocDescription) {
            super.javadoc = (MoJavadoc) value;
        } else if(description == modifiersDescription) {
            super.modifiers.clear();
            super.modifiers.addAll((MoNodeList<MoExtendedModifier>) value);
        } else if(description == typeDescription) {
            type = (MoType) value;
        } else if(description == fragmentsDescription) {
            fragments.clear();
            fragments.addAll((MoNodeList<MoVariableDeclarationFragment>) value);
        } else {
            logger.error("Role {} not found in MoFieldDeclaration", role);
        }
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoFieldDeclaration(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoFieldDeclaration otherFieldDeclaration) {
            boolean match;
            if(super.javadoc == null) {
                match = otherFieldDeclaration.javadoc == null;
            } else {
                match = super.javadoc.isSame(otherFieldDeclaration.javadoc);
            }
            match = match && MoExtendedModifier.sameList(super.modifiers, otherFieldDeclaration.modifiers);
            match = match && type.isSame(otherFieldDeclaration.type);
            match = match && MoNodeList.sameList(fragments, otherFieldDeclaration.fragments);
            return match;
        }
        return false;
    }
}
