package repair.ast.declaration;

import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.MoExtendedModifier;
import repair.ast.code.MoJavadoc;
import repair.ast.code.MoTypeParameter;
import repair.ast.code.expression.MoSimpleName;
import repair.ast.code.type.MoType;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MoEnumDeclaration extends MoAbstractTypeDeclaration{
    private static final Logger logger = LoggerFactory.getLogger(MoEnumDeclaration.class);
    @Serial
    private static final long serialVersionUID = -2977085964360361221L;

    // BodyDeclaration
    private final static Description<MoEnumDeclaration, MoJavadoc> javadocDescription =
            new Description<>(ChildType.CHILD, MoEnumDeclaration.class, MoJavadoc.class,
                    "javadoc", false);

    private final static Description<MoEnumDeclaration, MoExtendedModifier> modifiersDescription =
            new Description<>(ChildType.CHILDLIST, MoEnumDeclaration.class, MoExtendedModifier.class,
                    "modifiers", true);

    // AbstractTypeDeclaration
    private final static Description<MoEnumDeclaration, MoSimpleName> nameDescription =
            new Description<>(ChildType.CHILD, MoEnumDeclaration.class, MoSimpleName.class,
                    "name", true);

    private final static Description<MoEnumDeclaration, MoBodyDeclaration> bodyDeclarationsDescription =
            new Description<>(ChildType.CHILDLIST, MoEnumDeclaration.class, MoBodyDeclaration.class,
                    "bodyDeclarations", true);

    // TypeDeclaration
    private final static Description<MoEnumDeclaration, MoType> superInterfaceTypesDescription =
            new Description<>(ChildType.CHILDLIST, MoEnumDeclaration.class, MoType.class,
                    "superInterfaceTypes", true);

    private final static Description<MoEnumDeclaration, MoEnumConstantDeclaration> enumConstantsDescription =
            new Description<>(ChildType.CHILDLIST, MoEnumDeclaration.class, MoEnumConstantDeclaration.class,
                    "enumConstants", true);

    private final static Map<String, Description<MoEnumDeclaration, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("javadoc", javadocDescription),
            Map.entry("modifiers", modifiersDescription),
            Map.entry("name", nameDescription),
            Map.entry("bodyDeclarations", bodyDeclarationsDescription),
            Map.entry("superInterfaceTypes", superInterfaceTypesDescription),
            Map.entry("enumConstants", enumConstantsDescription)
    );

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "superInterfaceTypes", mandatory = true)
    private final MoNodeList<MoType> superInterfaceTypes;

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "enumConstants", mandatory = true)
    private final MoNodeList<MoEnumConstantDeclaration> enumConstants;

    public MoEnumDeclaration(String fileName, int startLine, int endLine, EnumDeclaration enumDeclaration) {
        super(fileName, startLine, endLine, enumDeclaration);
        moNodeType = MoNodeType.TYPEEnumDeclaration;
        super.modifiers = new MoNodeList<>(this, modifiersDescription);
        super.bodyDeclarations = new MoNodeList<>(this, bodyDeclarationsDescription);
        superInterfaceTypes = new MoNodeList<>(this, superInterfaceTypesDescription);
        enumConstants = new MoNodeList<>(this, enumConstantsDescription);
    }

    public void addSuperInterfaceType(MoType moType) {
        superInterfaceTypes.add(moType);
    }

    public void addEnumConstant(MoEnumConstantDeclaration moEnumConstantDeclaration) {
        enumConstants.add(moEnumConstantDeclaration);
    }

    public List<MoType> getSuperInterfaceTypes() {
        return superInterfaceTypes;
    }

    public List<MoEnumConstantDeclaration> getEnumConstants() {
        return enumConstants;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoEnumDeclaration(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoEnumDeclaration, ?> description = descriptionsMap.get(role);
        if(description == javadocDescription) {
            return super.javadoc;
        } else if(description == modifiersDescription) {
            return super.modifiers;
        } else if(description == nameDescription) {
            return super.name;
        } else if(description == bodyDeclarationsDescription) {
            return super.bodyDeclarations;
        } else if(description == superInterfaceTypesDescription) {
            return superInterfaceTypes;
        } else if(description == enumConstantsDescription) {
            return enumConstantsDescription;
        } else {
            logger.error("Role {} not found in MoEnumDeclaration", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoEnumDeclaration, ?> description = descriptionsMap.get(role);
        if(description == javadocDescription) {
            super.javadoc = (MoJavadoc) value;
        } else if(description == modifiersDescription) {
            super.modifiers.clear();
            super.modifiers.addAll((MoNodeList<MoExtendedModifier>) value);
        } else if(description == nameDescription) {
            super.name = (MoSimpleName) value;
        } else if(description == bodyDeclarationsDescription) {
            super.bodyDeclarations = (MoNodeList<MoBodyDeclaration>) value;
        } else if(description == superInterfaceTypesDescription) {
            superInterfaceTypes.clear();
            superInterfaceTypes.addAll((List<MoType>) value);
        } else if(description == enumConstantsDescription) {
            enumConstants.clear();
            enumConstants.addAll((List<MoEnumConstantDeclaration>) value);
        } else {
            logger.error("Role {} not found in MoEnumDeclaration", role);
        }
    }

    public static Map<String, Description<MoEnumDeclaration, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoEnumDeclaration(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoEnumDeclaration moEnumDeclaration) {
            boolean match;
            if(super.javadoc == null) {
                match = moEnumDeclaration.javadoc == null;
            } else {
                match = super.javadoc.isSame(moEnumDeclaration.javadoc);
            }
            match = match && MoExtendedModifier.sameList(super.modifiers, moEnumDeclaration.modifiers);
            match = match && super.name.isSame(moEnumDeclaration.name);
            match = match && MoNodeList.sameList(super.bodyDeclarations, moEnumDeclaration.bodyDeclarations);
            match = match && MoNodeList.sameList(this.superInterfaceTypes, moEnumDeclaration.superInterfaceTypes);
            match = match && MoNodeList.sameList(this.enumConstants, moEnumDeclaration.enumConstants);
            return match;
        }
        return false;
    }
}
