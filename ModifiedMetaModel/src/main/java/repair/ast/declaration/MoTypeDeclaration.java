package repair.ast.declaration;

import org.eclipse.jdt.core.dom.TypeDeclaration;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoTypeDeclaration extends MoAbstractTypeDeclaration{
    private static final Logger logger = LoggerFactory.getLogger(MoTypeDeclaration.class);
    @Serial
    private static final long serialVersionUID = -8198360919803443916L;

    // BodyDeclaration
    private final static Description<MoTypeDeclaration, MoJavadoc> javadocDescription =
            new Description<>(ChildType.CHILD, MoTypeDeclaration.class, MoJavadoc.class,
                    "javadoc", false);

    private final static Description<MoTypeDeclaration, MoExtendedModifier> modifiersDescription =
            new Description<>(ChildType.CHILDLIST, MoTypeDeclaration.class, MoExtendedModifier.class,
                    "modifiers", true);

    // AbstractTypeDeclaration
    private final static Description<MoTypeDeclaration, MoSimpleName> nameDescription =
            new Description<>(ChildType.CHILD, MoTypeDeclaration.class, MoSimpleName.class,
                    "name", true);

    private final static Description<MoTypeDeclaration, MoBodyDeclaration> bodyDeclarationsDescription =
            new Description<>(ChildType.CHILDLIST, MoTypeDeclaration.class, MoBodyDeclaration.class,
                    "bodyDeclarations", true);

    // TypeDeclaration
    private final static Description<MoTypeDeclaration, Boolean> interfaceDescription =
            new Description<>(ChildType.SIMPLE, MoTypeDeclaration.class, Boolean.class,
                    "interface", true);

    private final static Description<MoTypeDeclaration, MoType> superclassTypeDescription =
            new Description<>(ChildType.CHILD, MoTypeDeclaration.class, MoType.class,
                    "superclassType", false);

    private final static Description<MoTypeDeclaration, MoType> superInterfaceTypesDescription =
            new Description<>(ChildType.CHILDLIST, MoTypeDeclaration.class, MoType.class,
                    "superInterfaceTypes", true);

    private final static Description<MoTypeDeclaration, MoTypeParameter> typeParametersDescription =
            new Description<>(ChildType.CHILDLIST, MoTypeDeclaration.class, MoTypeParameter.class,
                    "typeParameters", true);

    private final static Map<String, Description<MoTypeDeclaration, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("javadoc", javadocDescription),
            Map.entry("modifiers", modifiersDescription),
            Map.entry("name", nameDescription),
            Map.entry("bodyDeclarations", bodyDeclarationsDescription),
            Map.entry("interface", interfaceDescription),
            Map.entry("superclassType", superclassTypeDescription),
            Map.entry("superInterfaceTypes", superInterfaceTypesDescription),
            Map.entry("typeParameters", typeParametersDescription)
    );

    @RoleDescriptor(type = ChildType.SIMPLE, role = "interface", mandatory = true)
    private boolean isInterface;
    @RoleDescriptor(type = ChildType.CHILD, role = "superclassType", mandatory = false)
    private MoType superclassType;
    @RoleDescriptor(type = ChildType.CHILDLIST, role = "superInterfaceTypes", mandatory = true)
    private final MoNodeList<MoType> superInterfaceTypes;
    @RoleDescriptor(type = ChildType.CHILDLIST, role = "typeParameters", mandatory = true)
    private final MoNodeList<MoTypeParameter> typeParameters;

    public MoTypeDeclaration(String fileName, int startLine, int endLine, TypeDeclaration typeDeclaration) {
        super(fileName, startLine, endLine, typeDeclaration);
        moNodeType = MoNodeType.TYPETypeDeclaration;
        super.modifiers = new MoNodeList<>(this, modifiersDescription);
        super.bodyDeclarations = new MoNodeList<>(this, bodyDeclarationsDescription);
        superInterfaceTypes = new MoNodeList<>(this, superInterfaceTypesDescription);
        typeParameters = new MoNodeList<>(this, typeParametersDescription);
    }

    public void setSuperclassType(MoType superclassType) {
        this.superclassType = superclassType;
    }

    public void addSuperinterfaceType(MoType superinterfaceType) {
        superInterfaceTypes.add(superinterfaceType);
    }

    public void addTypeParameter(MoTypeParameter typeParameter) {
        typeParameters.add(typeParameter);
    }

    public boolean isInterface() {
        return isInterface;
    }

    public Optional<MoType> getSuperclassType() {
        return Optional.ofNullable(superclassType);
    }

    public List<MoType> getSuperInterfaceTypes() {
        return superInterfaceTypes;
    }

    public List<MoTypeParameter> getTypeParameters() {
        return typeParameters;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoTypeDeclaration(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoTypeDeclaration, ?> description = descriptionsMap.get(role);
        if(description == javadocDescription) {
            return super.javadoc;
        } else if(description == modifiersDescription) {
            return super.modifiers;
        } else if(description == nameDescription) {
            return super.name;
        } else if(description == bodyDeclarationsDescription) {
            return super.bodyDeclarations;
        } else if(description == interfaceDescription) {
            return isInterface;
        } else if(description == superclassTypeDescription) {
            return superclassType;
        } else if(description == superInterfaceTypesDescription) {
            return superInterfaceTypes;
        } else if(description == typeParametersDescription) {
            return typeParameters;
        } else {
            logger.error("Role {} not found in MoTypeDeclaration", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoTypeDeclaration, ?> description = descriptionsMap.get(role);
        if(description == javadocDescription) {
            super.javadoc = (MoJavadoc) value;
        } else if(description == modifiersDescription) {
            super.modifiers.clear();
            super.modifiers.addAll((List<MoExtendedModifier>) value);
        } else if(description == nameDescription) {
            super.name = (MoSimpleName) value;
        } else if(description == bodyDeclarationsDescription) {
            super.bodyDeclarations.clear();
            super.bodyDeclarations.addAll((List<MoBodyDeclaration>) value);
        } else if(description == interfaceDescription) {
            isInterface = (boolean) value;
        } else if(description == superclassTypeDescription) {
            superclassType = (MoType) value;
        } else if(description == superInterfaceTypesDescription) {
            superInterfaceTypes.clear();
            superInterfaceTypes.addAll((List<MoType>) value);
        } else if(description == typeParametersDescription) {
            typeParameters.clear();
            typeParameters.addAll((List<MoTypeParameter>) value);
        } else {
            logger.error("Role {} not found in MoTypeDeclaration", role);
        }
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoTypeDeclaration clone = new MoTypeDeclaration(getFileName(), getStartLine(), getEndLine(), null);
        clone.setStructuralProperty("interface", isInterface());
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoTypeDeclaration moTypeDeclaration) {
            boolean match;
            if(javadoc == null) {
                match = moTypeDeclaration.javadoc == null;
            } else {
                match = javadoc.isSame(moTypeDeclaration.javadoc);
            }
            match = match && MoExtendedModifier.sameList(modifiers, moTypeDeclaration.modifiers);
            match = match && name.isSame(moTypeDeclaration.name);
            match = match && MoNodeList.sameList(bodyDeclarations, moTypeDeclaration.bodyDeclarations);
            match = match && isInterface == moTypeDeclaration.isInterface;
            match = match && superclassType.isSame(moTypeDeclaration.superclassType);
            match = match && MoNodeList.sameList(superInterfaceTypes, moTypeDeclaration.superInterfaceTypes);
            return match && MoNodeList.sameList(typeParameters, moTypeDeclaration.typeParameters);
        }
        return false;
    }
}
