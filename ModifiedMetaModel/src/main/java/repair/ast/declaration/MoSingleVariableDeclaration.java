package repair.ast.declaration;

import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.MoDimension;
import repair.ast.code.MoModifier;
import repair.ast.code.expression.MoAnnotation;
import repair.ast.code.MoExtendedModifier;
import repair.ast.code.expression.MoExpression;
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

public class MoSingleVariableDeclaration extends MoVariableDeclaration {
    private static final Logger logger = LoggerFactory.getLogger(MoSingleVariableDeclaration.class);
    @Serial
    private static final long serialVersionUID = -3351874403804006589L;

    private final static Description<MoSingleVariableDeclaration, MoSimpleName> nameDescription =
            new Description<>(ChildType.CHILD, MoSingleVariableDeclaration.class, MoSimpleName.class,
                    "name", true);

    private final static Description<MoSingleVariableDeclaration, MoDimension> extraDimensionsDescription =
            new Description<>(ChildType.CHILDLIST, MoSingleVariableDeclaration.class, MoDimension.class,
                    "extraDimensions2", true);

    private final static Description<MoSingleVariableDeclaration, MoExpression> initializerDescription =
            new Description<>(ChildType.CHILD, MoSingleVariableDeclaration.class, MoExpression.class,
                    "initializer", false);


    private final static Description<MoSingleVariableDeclaration, MoExtendedModifier> modifiersDescription =
            new Description<>(ChildType.CHILDLIST, MoSingleVariableDeclaration.class, MoExtendedModifier.class,
                    "modifiers", true);

    private final static Description<MoSingleVariableDeclaration, MoType> typeDescription =
            new Description<>(ChildType.CHILD, MoSingleVariableDeclaration.class, MoType.class,
                    "type", true);

    private final static Description<MoSingleVariableDeclaration, MoAnnotation> varargsAnnotationsDescription =
            new Description<>(ChildType.CHILDLIST, MoSingleVariableDeclaration.class, MoAnnotation.class,
                    "varargsAnnotations", true);

    private final static Description<MoSingleVariableDeclaration, Boolean> varargsDescription =
            new Description<>(ChildType.SIMPLE, MoSingleVariableDeclaration.class, Boolean.class,
                    "varargs", true);

    private final static Map<String, Description<MoSingleVariableDeclaration, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("name", nameDescription),
            Map.entry("extraDimensions2", extraDimensionsDescription),
            Map.entry("initializer", initializerDescription),
            Map.entry("modifiers", modifiersDescription),
            Map.entry("type", typeDescription),
            Map.entry("varargsAnnotations", varargsAnnotationsDescription),
            Map.entry("varargs", varargsDescription)
    );

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "modifiers", mandatory = true)
    private final MoNodeList<MoExtendedModifier> modifiers;
    @RoleDescriptor(type = ChildType.CHILD, role = "type", mandatory = true)
    private MoType type;
    @RoleDescriptor(type = ChildType.CHILDLIST, role = "varargsAnnotations", mandatory = true)
    private final MoNodeList<MoAnnotation> varargsAnnotations;

    @RoleDescriptor(type = ChildType.SIMPLE, role = "varargs", mandatory = true)
    private boolean isVarargs;

    public MoSingleVariableDeclaration(String fileName, int startLine, int endLine, SingleVariableDeclaration singleVariableDeclaration) {
        super(fileName, startLine, endLine, singleVariableDeclaration);
        moNodeType = MoNodeType.TYPESingleVariableDeclaration;
        super.CStyleArrayDimensions = new MoNodeList<>(this, extraDimensionsDescription);
        modifiers = new MoNodeList<>(this, modifiersDescription);
        varargsAnnotations = new MoNodeList<>(this, varargsAnnotationsDescription);
    }

    public void addModifier(MoExtendedModifier modifier) {
        modifiers.add(modifier);
    }

    public void setType(MoType type) {
        this.type = type;
    }

    public void setVarargs(boolean isVarargs) {
        this.isVarargs = isVarargs;
    }

    public void addVarargsAnnotation(MoAnnotation annotation) {
        varargsAnnotations.add(annotation);
    }

    public List<MoExtendedModifier> getModifiers() {
        return modifiers;
    }

    public MoType getType() {
        return type;
    }

    public boolean isVarargs() {
        return isVarargs;
    }

    public List<MoAnnotation> getVarargsAnnotations() {
        return varargsAnnotations;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoSingleVariableDeclaration(this);
    }

    @Override
    public List<MoNode> getChildren() {
        List<MoNode> children = new ArrayList<>();
        children.add(name);
        modifiers.forEach(modifier -> children.add(((MoNode) modifier)));
        children.add(type);
        children.addAll(varargsAnnotations);
        children.addAll(CStyleArrayDimensions);
        if(initializer != null) {
            children.add(initializer);
        }
        return Collections.unmodifiableList(children);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoSingleVariableDeclaration, ?> description = descriptionsMap.get(role);
        if(description == nameDescription) {
            return super.name;
        } else if(description == extraDimensionsDescription) {
            return super.CStyleArrayDimensions;
        } else if(description == initializerDescription) {
            return super.initializer;
        } else if(description == modifiersDescription) {
            return modifiers;
        } else if(description == typeDescription) {
            return type;
        } else if(description == varargsAnnotationsDescription) {
            return varargsAnnotations;
        } else if(description == varargsDescription) {
            return isVarargs;
        } else {
            logger.error("Role {} not found in MoSingleVariableDeclaration", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoSingleVariableDeclaration, ?> description = descriptionsMap.get(role);
        if(description == nameDescription) {
            super.name = (MoSimpleName) value;
        } else if(description == extraDimensionsDescription) {
            super.CStyleArrayDimensions.clear();
            super.CStyleArrayDimensions.addAll((List<MoDimension>) value);
        } else if(description == initializerDescription) {
            super.initializer = (MoExpression) value;
        } else if(description == modifiersDescription) {
            modifiers.clear();
            modifiers.addAll((List<MoExtendedModifier>) value);
        } else if(description == typeDescription) {
            type = (MoType) value;
        } else if(description == varargsAnnotationsDescription) {
            varargsAnnotations.clear();
            varargsAnnotations.addAll((List<MoAnnotation>) value);
        } else if(description == varargsDescription) {
            isVarargs = (boolean) value;
        } else {
            logger.error("Role {} not found in MoSingleVariableDeclaration", role);
        }
    }

    public static Map<String, Description<MoSingleVariableDeclaration, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoSingleVariableDeclaration clone = new MoSingleVariableDeclaration(getFileName(), getStartLine(), getEndLine(), null);
        clone.setStructuralProperty("varargs", isVarargs());
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoSingleVariableDeclaration otherSingleVariableDeclaration) {
            boolean match = MoExtendedModifier.sameList(this.modifiers, otherSingleVariableDeclaration.modifiers);
            match = match && this.type.isSame(otherSingleVariableDeclaration.type);
            match = match && this.name.isSame(otherSingleVariableDeclaration.name);
            match = match && this.isVarargs == otherSingleVariableDeclaration.isVarargs;
            match = match && MoExtendedModifier.sameList(this.varargsAnnotations, otherSingleVariableDeclaration.varargsAnnotations);
            match = match && MoNodeList.sameList(this.CStyleArrayDimensions, otherSingleVariableDeclaration.CStyleArrayDimensions);
            if(this.initializer == null && otherSingleVariableDeclaration.initializer == null) {
                return match;
            } else if(this.initializer != null && otherSingleVariableDeclaration.initializer != null) {
                return match && this.initializer.isSame(otherSingleVariableDeclaration.initializer);
            }
        }
        return false;
    }
}
