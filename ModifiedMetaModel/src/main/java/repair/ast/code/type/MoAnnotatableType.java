package repair.ast.code.type;

import org.eclipse.jdt.core.dom.AnnotatableType;
import repair.ast.MoNodeList;
import repair.ast.code.expression.MoAnnotation;
import repair.ast.role.ChildType;
import repair.ast.role.RoleDescriptor;

import java.io.Serial;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public abstract class MoAnnotatableType extends MoType {
    @Serial
    private static final long serialVersionUID = 8582819693857302837L;

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "annotations", mandatory = true)
    protected MoNodeList<MoAnnotation> annotations;

    public MoAnnotatableType(Path fileName, int startLine, int endLine, AnnotatableType typeNode) {
        super(fileName, startLine, endLine, typeNode);
    }

    public void addAnnotation(MoAnnotation annotation) {
        annotations.add(annotation);
    }
    public List<MoAnnotation> getAnnotations() {
        return annotations;
    }

}
