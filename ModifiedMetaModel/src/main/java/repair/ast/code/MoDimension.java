package repair.ast.code;

import org.eclipse.jdt.core.dom.Dimension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.expression.MoAnnotation;
import repair.ast.code.statement.MoVariableDeclarationStatement;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MoDimension extends MoNode {
    private static final Logger logger = LoggerFactory.getLogger(MoDimension.class);
    @Serial
    private static final long serialVersionUID = -3283524675454823989L;

    private final static Description<MoDimension, MoAnnotation> annotationsDescription =
            new Description<>(ChildType.CHILDLIST, MoDimension.class, MoAnnotation.class,
                    "annotations", true);

    private final static Map<String, Description<MoDimension, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("annotations", annotationsDescription)
    );

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "annotations", mandatory = true)
    private final MoNodeList<MoAnnotation> annotations;

    public MoDimension(String fileName, int startLine, int endLine, Dimension dimension) {
        super(fileName, startLine, endLine, dimension);
        moNodeType = MoNodeType.TYPEDimension;
        annotations = new MoNodeList<>(this, annotationsDescription);
    }

    public void addAnnotation(MoAnnotation annotation) {
        annotations.add(annotation);
    }

    public List<MoAnnotation> getAnnotations() {
        return annotations;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoDimension(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoDimension, ?> description = descriptionsMap.get(role);
        if(description == annotationsDescription) {
            return annotations;
        } else {
            logger.error("Role {} not found in MoDimension", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoDimension, ?> description = descriptionsMap.get(role);
        if(description == annotationsDescription) {
            List<MoAnnotation> annotations = (List<MoAnnotation>) value;
            this.annotations.clear();
            this.annotations.addAll(annotations);
        } else {
            logger.error("Role {} not found in MoDimension", role);
        }
    }

    public static Map<String, Description<MoDimension, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoDimension(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoDimension moDimension) {
            return MoExtendedModifier.sameList(annotations, moDimension.annotations);
        }
        return false;
    }
}
