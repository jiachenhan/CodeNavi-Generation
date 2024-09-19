package repair.ast.declaration;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.MoExtendedModifier;
import repair.ast.code.MoJavadoc;
import repair.ast.code.MoTypeParameter;
import repair.ast.code.expression.MoSimpleName;
import repair.ast.code.statement.MoBlock;
import repair.ast.code.type.MoType;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.*;

public class MoMethodDeclaration extends MoBodyDeclaration {
    private static final Logger logger = LoggerFactory.getLogger(MoMethodDeclaration.class);
    @Serial
    private static final long serialVersionUID = 5854414223079912413L;

    private final static Description<MoMethodDeclaration, MoJavadoc> javadocDescription =
            new Description<>(ChildType.CHILD, MoMethodDeclaration.class, MoJavadoc.class,
                    "javadoc", false);

    private final static Description<MoMethodDeclaration, MoExtendedModifier> modifiersDescription =
            new Description<>(ChildType.CHILDLIST, MoMethodDeclaration.class, MoExtendedModifier.class,
                    "modifiers", true);

    private final static Description<MoMethodDeclaration, Boolean> constructorDescription =
            new Description<>(ChildType.SIMPLE, MoMethodDeclaration.class, Boolean.class,
                    "constructor", true);

    private final static Description<MoMethodDeclaration, MoSimpleName> nameDescription =
            new Description<>(ChildType.CHILD, MoMethodDeclaration.class, MoSimpleName.class,
                    "name", true);

    private final static Description<MoMethodDeclaration, MoType> returnTypeDescription =
            new Description<>(ChildType.CHILD, MoMethodDeclaration.class, MoType.class,
                    "returnType2", false);

    private final static Description<MoMethodDeclaration, MoTypeParameter> typeParametersDescription =
            new Description<>(ChildType.CHILDLIST, MoMethodDeclaration.class, MoTypeParameter.class,
                    "typeParameters", true);

    private final static Description<MoMethodDeclaration, MoSingleVariableDeclaration> parametersDescription =
            new Description<>(ChildType.CHILDLIST, MoMethodDeclaration.class, MoSingleVariableDeclaration.class,
                    "parameters", true);

    private final static Description<MoMethodDeclaration, MoType> thrownExceptionTypesDescription =
            new Description<>(ChildType.CHILDLIST, MoMethodDeclaration.class, MoType.class,
                    "thrownExceptionTypes", true);

    private final static Description<MoMethodDeclaration, MoBlock> bodyDescription =
            new Description<>(ChildType.CHILD, MoMethodDeclaration.class, MoBlock.class,
                    "body", false);

    private final static Map<String, Description<MoMethodDeclaration, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("javadoc", javadocDescription),
            Map.entry("modifiers", modifiersDescription),
            Map.entry("constructor", constructorDescription),
            Map.entry("name", nameDescription),
            Map.entry("returnType2", returnTypeDescription),
            Map.entry("typeParameters", typeParametersDescription),
            Map.entry("parameters", parametersDescription),
            Map.entry("thrownExceptionTypes", thrownExceptionTypesDescription),
            Map.entry("body", bodyDescription)
    );

    @RoleDescriptor(type = ChildType.SIMPLE, role = "constructor", mandatory = true)
    private boolean isConstructor;
    @RoleDescriptor(type = ChildType.CHILD, role = "name", mandatory = true)
    private MoSimpleName name;

    @RoleDescriptor(type = ChildType.CHILD, role = "returnType2", mandatory = false)
    private MoType returnType;

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "typeParameters", mandatory = true)
    private final MoNodeList<MoTypeParameter> typeParameters;

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "parameters", mandatory = true)
    private final MoNodeList<MoSingleVariableDeclaration> parameters;

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "thrownExceptionTypes", mandatory = true)
    private final MoNodeList<MoType> thrownExceptionTypes;

    @RoleDescriptor(type = ChildType.CHILD, role = "body", mandatory = false)
    private MoBlock body;

    public MoMethodDeclaration(String fileName, int startLine, int endLine, MethodDeclaration methodDeclaration) {
        super(fileName, startLine, endLine, methodDeclaration);
        moNodeType = MoNodeType.TYPEMethodDeclaration;
        super.modifiers = new MoNodeList<>(this, modifiersDescription);
        this.typeParameters = new MoNodeList<>(this, typeParametersDescription);
        this.parameters = new MoNodeList<>(this, parametersDescription);
        this.thrownExceptionTypes = new MoNodeList<>(this, thrownExceptionTypesDescription);
    }

    public void setConstructor(boolean constructor) {
        isConstructor = constructor;
    }
    public void setName(MoSimpleName name) {
        this.name = name;
    }

    public void setReturnType(MoType returnType) {
        this.returnType = returnType;
    }

    public void addTypeParameter(MoTypeParameter typeParameter) {
        typeParameters.add(typeParameter);
    }

    public void addParameter(MoSingleVariableDeclaration parameter) {
        parameters.add(parameter);
    }

    private void addThrownExceptionType(MoType thrownExceptionType) {
        thrownExceptionTypes.add(thrownExceptionType);
    }

    public void setBody(MoBlock body) {
        this.body = body;
    }

    public boolean isConstructor() {
        return isConstructor;
    }

    public MoSimpleName getName() {
        return name;
    }

    public Optional<MoType> getReturnType() {
        return Optional.ofNullable(returnType);
    }

    public List<MoTypeParameter> getTypeParameters() {
        return typeParameters;
    }

    public List<MoSingleVariableDeclaration> getParameters() {
        return parameters;
    }

    public List<MoType> getThrownExceptionTypes() {
        return thrownExceptionTypes;
    }

    public Optional<MoBlock> getBody() {
        return Optional.ofNullable(body);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoMethodDeclaration(this);
    }

    @Override
    public List<MoNode> getChildren() {
        List<MoNode> children = new ArrayList<>();
        if(super.javadoc != null) {
            children.add(super.javadoc);
        }
        super.modifiers.forEach(modifier -> children.add(((MoNode) modifier)));
        children.add(name);
        if(returnType != null) {
            children.add(returnType);
        }
        children.addAll(typeParameters);
        children.addAll(parameters);
        children.addAll(thrownExceptionTypes);
        if(body != null) {
            children.add(body);
        }
        return Collections.unmodifiableList(children);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoMethodDeclaration, ?> description = descriptionsMap.get(role);
        if(description == javadocDescription) {
            return super.javadoc;
        } else if(description == modifiersDescription) {
            return super.modifiers;
        } else if(description == constructorDescription) {
            return isConstructor;
        } else if(description == nameDescription) {
            return name;
        } else if(description == returnTypeDescription) {
            return returnType;
        } else if(description == typeParametersDescription) {
            return typeParameters;
        } else if(description == parametersDescription) {
            return parameters;
        } else if(description == thrownExceptionTypesDescription) {
            return thrownExceptionTypes;
        } else if(description == bodyDescription) {
            return body;
        } else {
            logger.error("Role {} not found in MoMethodDeclaration", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoMethodDeclaration, ?> description = descriptionsMap.get(role);
        if(description == javadocDescription) {
            super.javadoc = (MoJavadoc) value;
        } else if(description == modifiersDescription) {
            super.modifiers.clear();
            super.modifiers.addAll((List<MoExtendedModifier>) value);
        } else if(description == constructorDescription) {
            isConstructor = (boolean) value;
        } else if(description == nameDescription) {
            name = (MoSimpleName) value;
        } else if(description == returnTypeDescription) {
            returnType = (MoType) value;
        } else if(description == typeParametersDescription) {
            typeParameters.clear();
            typeParameters.addAll((List<MoTypeParameter>) value);
        } else if(description == parametersDescription) {
            parameters.clear();
            parameters.addAll((List<MoSingleVariableDeclaration>) value);
        } else if(description == thrownExceptionTypesDescription) {
            thrownExceptionTypes.clear();
            thrownExceptionTypes.addAll((List<MoType>) value);
        } else if(description == bodyDescription) {
            body = (MoBlock) value;
        } else {
            logger.error("Role {} not found in MoMethodDeclaration", role);
        }
    }

    public static Map<String, Description<MoMethodDeclaration, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoMethodDeclaration clone = new MoMethodDeclaration(getFileName(), getStartLine(), getEndLine(), null);
        clone.setStructuralProperty("constructor", isConstructor());
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoMethodDeclaration otherMethodDeclaration) {
            boolean match;
            if(super.javadoc == null) {
                match = otherMethodDeclaration.javadoc == null;
            } else {
                match = super.javadoc.isSame(otherMethodDeclaration.javadoc);
            }
            match = match && MoExtendedModifier.sameList(super.modifiers, otherMethodDeclaration.modifiers);
            match = match && isConstructor == otherMethodDeclaration.isConstructor;
            match = match && name.isSame(otherMethodDeclaration.name);
            if(returnType == null) {
                match = match && otherMethodDeclaration.returnType == null;
            } else {
                match = match && returnType.isSame(otherMethodDeclaration.returnType);
            }
            match = match && MoNodeList.sameList(typeParameters, otherMethodDeclaration.typeParameters);
            match = match && MoNodeList.sameList(parameters, otherMethodDeclaration.parameters);
            match = match && MoNodeList.sameList(thrownExceptionTypes, otherMethodDeclaration.thrownExceptionTypes);
            if(body == null) {
                match = match && otherMethodDeclaration.body == null;
            } else {
                match = match && body.isSame(otherMethodDeclaration.body);
            }
            return match;
        }
        return false;
    }
}
