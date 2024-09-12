package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.ArrayCreation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.type.MoArrayType;
import repair.ast.declaration.MoAnonymousClassDeclaration;
import repair.ast.declaration.MoBodyDeclaration;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MoArrayCreation extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoArrayCreation.class);
    @Serial
    private static final long serialVersionUID = 2383561270570871014L;

    private final static Description<MoArrayCreation, MoArrayType> typeDescription =
            new Description<>(ChildType.CHILD, MoArrayCreation.class, MoArrayType.class,
                    "type", true);

    private final static Description<MoArrayCreation, MoExpression> dimensionDescription =
            new Description<>(ChildType.CHILDLIST, MoArrayCreation.class, MoExpression.class,
                    "dimensions", true);

    private final static Description<MoArrayCreation, MoArrayInitializer> initializerDescription =
            new Description<>(ChildType.CHILD, MoArrayCreation.class, MoArrayInitializer.class,
                    "initializer", false);

    private final static Map<String, Description<MoArrayCreation, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("type", typeDescription),
            Map.entry("dimensions", dimensionDescription),
            Map.entry("initializer", initializerDescription)
    );


    @RoleDescriptor(type = ChildType.CHILD, role = "type", mandatory = true)
    private MoArrayType type;

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "dimensions", mandatory = true)
    private final MoNodeList<MoExpression> dimensionExpressions;

    @RoleDescriptor(type = ChildType.CHILD, role = "initializer", mandatory = false)
    private MoArrayInitializer initializer;

    public MoArrayCreation(String fileName, int startLine, int endLine, ArrayCreation arrayCreation) {
        super(fileName, startLine, endLine, arrayCreation);
        moNodeType = MoNodeType.TYPEArrayCreation;
        dimensionExpressions = new MoNodeList<>(this, dimensionDescription);
    }

    public void setType(MoArrayType type) {
        this.type = type;
    }

    public void addDimensionExpression(MoExpression dimensionExpression) {
        dimensionExpressions.add(dimensionExpression);
    }

    public void setInitializer(MoArrayInitializer initializer) {
        this.initializer = initializer;
    }

    public MoArrayType getType() {
        return type;
    }

    public List<MoExpression> getDimensionExpressions() {
        return dimensionExpressions;
    }

    public Optional<MoArrayInitializer> getInitializer() {
        return Optional.ofNullable(initializer);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoArrayCreation(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoArrayCreation, ?> description = descriptionsMap.get(role);
        if(description == typeDescription) {
            return type;
        } else if(description == dimensionDescription) {
            return dimensionExpressions;
        } else if(description == initializerDescription) {
            return initializer;
        } else {
            logger.error("Role {} not found in MoArrayCreation", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoArrayCreation, ?> description = descriptionsMap.get(role);
        if(description == typeDescription) {
            type = (MoArrayType) value;
        } else if(description == dimensionDescription) {
            dimensionExpressions.clear();
            dimensionExpressions.addAll((List<MoExpression>) value);
        } else if(description == initializerDescription) {
            initializer = (MoArrayInitializer) value;
        } else {
            logger.error("Role {} not found in MoArrayCreation", role);
        }
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoArrayCreation(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if (other instanceof MoArrayCreation arrayCreation) {
            boolean match = type.isSame(arrayCreation.type);
            match = match && MoNodeList.sameList(dimensionExpressions, arrayCreation.dimensionExpressions);
            if (initializer == null) {
                match = match && (arrayCreation.initializer == null);
            } else {
                match = match && initializer.isSame(arrayCreation.initializer);
            }
            return match;
        }
        return false;
    }
}
