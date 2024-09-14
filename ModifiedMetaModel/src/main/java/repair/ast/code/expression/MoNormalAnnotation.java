package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.MoMemberValuePair;
import repair.ast.code.statement.MoVariableDeclarationStatement;
import repair.ast.code.type.MoType;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MoNormalAnnotation extends MoAnnotation {
    private static final Logger logger = LoggerFactory.getLogger(MoNormalAnnotation.class);
    @Serial
    private static final long serialVersionUID = -7750924135316609932L;

    private final static Description<MoNormalAnnotation, MoName> typeNameDescription =
            new Description<>(ChildType.CHILD, MoNormalAnnotation.class, MoName.class,
                    "typeName", true);

    private final static Description<MoNormalAnnotation, MoMemberValuePair> valuesDescription =
            new Description<>(ChildType.CHILDLIST, MoNormalAnnotation.class, MoMemberValuePair.class,
                    "values", true);

    private final static Map<String, Description<MoNormalAnnotation, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("typeName", typeNameDescription),
            Map.entry("values", valuesDescription)
    );

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "values", mandatory = true)
    private final MoNodeList<MoMemberValuePair> memberValuePairs;

    public MoNormalAnnotation(String fileName, int startLine, int endLine, NormalAnnotation normalAnnotation) {
        super(fileName, startLine, endLine, normalAnnotation);
        moNodeType = MoNodeType.TYPENormalAnnotation;
        memberValuePairs = new MoNodeList<>(this, valuesDescription);
    }

    public void addMemberValuePair(MoMemberValuePair memberValuePair) {
        memberValuePairs.add(memberValuePair);
    }

    public List<MoMemberValuePair> getMemberValuePairs() {
        return memberValuePairs;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoNormalAnnotation(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoNormalAnnotation, ?> description = descriptionsMap.get(role);
        if(description == typeNameDescription) {
            return super.typeName;
        } else if(description == valuesDescription) {
            return memberValuePairs;
        } else {
            logger.error("Role {} not found in MoNormalAnnotation", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoNormalAnnotation, ?> description = descriptionsMap.get(role);
        if(description == typeNameDescription) {
            super.typeName = (MoName) value;
        } else if(description == valuesDescription) {
            memberValuePairs.clear();
            memberValuePairs.addAll((List<MoMemberValuePair>) value);
        } else {
            logger.error("Role {} not found in MoNormalAnnotation", role);
        }
    }

    public static Map<String, Description<MoNormalAnnotation, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoNormalAnnotation(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoNormalAnnotation otherNormalAnnotation) {
            return super.typeName.isSame(otherNormalAnnotation.typeName) &&
                    MoNodeList.sameList(memberValuePairs, otherNormalAnnotation.memberValuePairs);
        }
        return false;
    }
}
