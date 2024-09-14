package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.ThisExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.declaration.MoFieldDeclaration;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.Map;
import java.util.Optional;

public class MoThisExpression extends MoExpression {
    private static final Logger logger = LoggerFactory.getLogger(MoThisExpression.class);
    @Serial
    private static final long serialVersionUID = -2073032029671077131L;

    private final static Description<MoThisExpression, MoName> qualifierDescription =
            new Description<>(ChildType.CHILD, MoThisExpression.class, MoName.class,
                    "qualifier", false);

    private final static Map<String, Description<MoThisExpression, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("qualifier", qualifierDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "qualifier", mandatory = false)
    private MoName qualifier;

    public MoThisExpression(String fileName, int startLine, int endLine, ThisExpression thisExpression) {
        super(fileName, startLine, endLine, thisExpression);
        moNodeType = MoNodeType.TYPEThisExpression;
    }

    public void setQualifier(MoName qualifier) {
        this.qualifier = qualifier;
    }

    public Optional<MoName> getQualifier() {
        return Optional.ofNullable(qualifier);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoThisExpression(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoThisExpression, ?> description = descriptionsMap.get(role);
        if(description == qualifierDescription) {
            return qualifier;
        } else {
            logger.error("Role {} not found in MoThisExpression", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoThisExpression, ?> description = descriptionsMap.get(role);
        if(description == qualifierDescription) {
            qualifier = (MoName) value;
        } else {
            logger.error("Role {} not found in MoThisExpression", role);
        }
    }

    public static Map<String, Description<MoThisExpression, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoThisExpression(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoThisExpression otherThisExpression) {
            if(qualifier == null) {
                return otherThisExpression.qualifier == null;
            } else {
                return qualifier.isSame(otherThisExpression.qualifier);
            }
        }
        return false;
    }
}
