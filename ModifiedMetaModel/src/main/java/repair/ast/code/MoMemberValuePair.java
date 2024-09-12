package repair.ast.code;

import org.eclipse.jdt.core.dom.MemberValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoExpression;
import repair.ast.code.expression.MoSimpleName;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.Map;

public class MoMemberValuePair extends MoNode {
    private static final Logger logger = LoggerFactory.getLogger(MoMemberValuePair.class);
    @Serial
    private static final long serialVersionUID = -2309872060908832686L;

    private final static Description<MoMemberValuePair, MoSimpleName> nameDescription =
            new Description<>(ChildType.CHILD, MoMemberValuePair.class, MoSimpleName.class,
                    "name", true);

    private final static Description<MoMemberValuePair, MoExpression> valueDescription =
            new Description<>(ChildType.CHILD, MoMemberValuePair.class, MoExpression.class,
                    "value", true);

    private final static Map<String, Description<MoMemberValuePair, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("name", nameDescription),
            Map.entry("value", valueDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "name", mandatory = true)
    private MoSimpleName name;
    @RoleDescriptor(type = ChildType.CHILD, role = "value", mandatory = true)
    private MoExpression value;

    public MoMemberValuePair(String fileName, int startLine, int endLine, MemberValuePair memberValuePair) {
        super(fileName, startLine, endLine, memberValuePair);
        moNodeType = MoNodeType.TYPEMemberValuePair;
    }

    public void setName(MoSimpleName name) {
        this.name = name;
    }

    public void setValue(MoExpression value) {
        this.value = value;
    }

    public MoSimpleName getName() {
        return name;
    }

    public MoExpression getValue() {
        return value;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoMemberValuePair(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoMemberValuePair, ?> description = descriptionsMap.get(role);
        if(description == nameDescription) {
            return name;
        } else if(description == valueDescription) {
            return value;
        } else {
            logger.error("Role {} not found in MoMemberValuePair", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoMemberValuePair, ?> description = descriptionsMap.get(role);
        if(description == nameDescription) {
            name = (MoSimpleName) value;
        } else if(description == valueDescription) {
            this.value = (MoExpression) value;
        } else {
            logger.error("Role {} not found in MoMemberValuePair", role);
        }
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoMemberValuePair(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoMemberValuePair otherMemberValuePair) {
            return name.isSame(otherMemberValuePair.name) &&
                    value.isSame(otherMemberValuePair.value);
        }
        return false;
    }
}
