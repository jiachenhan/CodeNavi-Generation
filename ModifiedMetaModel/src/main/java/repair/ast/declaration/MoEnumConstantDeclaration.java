package repair.ast.declaration;

import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.MoExtendedModifier;
import repair.ast.code.MoJavadoc;
import repair.ast.code.expression.MoExpression;
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
import java.util.Optional;

/**
 *  EnumConstantDeclaration:
 *       [ Javadoc ] { ExtendedModifier } Identifier
 *           [ ( [ Expression { , Expression } ] ) ]
 *           [ AnonymousClassDeclaration ]
 */
public class MoEnumConstantDeclaration extends MoBodyDeclaration {
    private static final Logger logger = LoggerFactory.getLogger(MoEnumConstantDeclaration.class);
    @Serial
    private static final long serialVersionUID = -1557490023059955647L;

    private final static Description<MoEnumConstantDeclaration, MoJavadoc> javadocDescription =
            new Description<>(ChildType.CHILD, MoEnumConstantDeclaration.class, MoJavadoc.class,
                    "javadoc", false);

    private final static Description<MoEnumConstantDeclaration, MoExtendedModifier> modifiersDescription =
            new Description<>(ChildType.CHILDLIST, MoEnumConstantDeclaration.class, MoExtendedModifier.class,
                    "modifiers", true);

    private final static Description<MoEnumConstantDeclaration, MoSimpleName> nameDescription =
            new Description<>(ChildType.CHILD, MoEnumConstantDeclaration.class, MoSimpleName.class,
                    "name", true);

    private final static Description<MoEnumConstantDeclaration, MoExpression> argumentsDescription =
            new Description<>(ChildType.CHILDLIST, MoEnumConstantDeclaration.class, MoExpression.class,
                    "arguments", true);

    private final static Description<MoEnumConstantDeclaration, MoAnonymousClassDeclaration> anonymousClassDeclDescription =
            new Description<>(ChildType.CHILD, MoEnumConstantDeclaration.class, MoAnonymousClassDeclaration.class,
                    "anonymousClassDeclaration", false);

    private final static Map<String, Description<MoEnumConstantDeclaration, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("javadoc", javadocDescription),
            Map.entry("modifiers", modifiersDescription),
            Map.entry("name", nameDescription),
            Map.entry("arguments", argumentsDescription),
            Map.entry("anonymousClassDeclaration", anonymousClassDeclDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "name", mandatory = true)
    private MoSimpleName name;

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "arguments", mandatory = true)
    private final MoNodeList<MoExpression> arguments;

    @RoleDescriptor(type = ChildType.CHILD, role = "anonymousClassDeclaration", mandatory = false)
    private MoAnonymousClassDeclaration anonymousClassDeclaration;

    public MoEnumConstantDeclaration(String fileName, int startLine, int endLine, EnumConstantDeclaration enumConstantDeclaration) {
        super(fileName, startLine, endLine, enumConstantDeclaration);
        moNodeType = MoNodeType.TYPEEnumConstantDeclaration;
        super.modifiers = new MoNodeList<>(this, modifiersDescription);
        arguments = new MoNodeList<>(this, argumentsDescription);
    }


    public void setName(MoSimpleName name) {
        this.name = name;
    }

    public void addArgument(MoExpression argument) {
        arguments.add(argument);
    }

    public void setAnonymousClassDeclaration(MoAnonymousClassDeclaration anonymousClassDeclaration) {
        this.anonymousClassDeclaration = anonymousClassDeclaration;
    }

    public MoSimpleName getName() {
        return name;
    }

    public List<MoExpression> getArguments() {
        return arguments;
    }

    public Optional<MoAnonymousClassDeclaration> getAnonymousClassDeclaration() {
        return Optional.ofNullable(anonymousClassDeclaration);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoEnumConstantDeclaration(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoEnumConstantDeclaration, ?> description = descriptionsMap.get(role);
        if(description == javadocDescription) {
            return super.javadoc;
        } else if(description == modifiersDescription) {
            return super.modifiers;
        } else if(description == nameDescription) {
            return name;
        } else if(description == argumentsDescription) {
            return arguments;
        } else if(description == anonymousClassDeclDescription) {
            return anonymousClassDeclaration;
        } else {
            logger.error("Role {} not found in MoEnumConstantDeclaration", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoEnumConstantDeclaration, ?> description = descriptionsMap.get(role);
        if(description == javadocDescription) {
            super.javadoc = (MoJavadoc) value;
        } else if(description == modifiersDescription) {
            super.modifiers.clear();
            super.modifiers.addAll((List<MoExtendedModifier>) value);
        } else if(description == nameDescription) {
            this.name = (MoSimpleName) value;
        } else if(description == argumentsDescription) {
            this.arguments.clear();
            this.arguments.addAll((List<MoExpression>) value);
        } else if(description == anonymousClassDeclDescription) {
            this.anonymousClassDeclaration = (MoAnonymousClassDeclaration) value;
        } else {
            logger.error("Role {} not found in MoEnumConstantDeclaration", role);
        }
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoEnumConstantDeclaration(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoEnumConstantDeclaration moEnumConstantDeclaration) {
            boolean match;
            if (javadoc == null) {
                match = moEnumConstantDeclaration.javadoc == null;
            } else {
                match = javadoc.isSame(moEnumConstantDeclaration.javadoc);
            }
            match = match && MoExtendedModifier.sameList(modifiers, moEnumConstantDeclaration.modifiers);
            match = match && name.isSame(moEnumConstantDeclaration.name);
            match = match && MoNodeList.sameList(arguments, moEnumConstantDeclaration.arguments);
            if (anonymousClassDeclaration == null) {
                match = match && moEnumConstantDeclaration.anonymousClassDeclaration == null;
            } else {
                match = match && anonymousClassDeclaration.isSame(moEnumConstantDeclaration.anonymousClassDeclaration);
            }
            return match;
        }
        return false;
    }
}
