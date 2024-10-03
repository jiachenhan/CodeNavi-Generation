package repair.ast.code.expression;

import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.MoMemberValuePair;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class MoMarkerAnnotation extends MoAnnotation {
    private static final Logger logger = LoggerFactory.getLogger(MoMarkerAnnotation.class);
    @Serial
    private static final long serialVersionUID = 5305377024697002489L;

    private final static Description<MoMarkerAnnotation, MoName> typeNameDescription =
            new Description<>(ChildType.CHILD, MoMarkerAnnotation.class, MoName.class,
                    "typeName", true);

    private final static Map<String, Description<MoMarkerAnnotation, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("typeName", typeNameDescription)
    );


    public MoMarkerAnnotation(Path fileName, int startLine, int endLine, MarkerAnnotation markerAnnotation) {
        super(fileName, startLine, endLine, markerAnnotation);
        moNodeType = MoNodeType.TYPEMarkerAnnotation;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoMarkerAnnotation(this);
    }

    @Override
    public List<MoNode> getChildren() {
        return List.of(typeName);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoMarkerAnnotation, ?> description = descriptionsMap.get(role);
        if(description == typeNameDescription) {
            return super.typeName;
        } else {
            logger.error("Role {} not found in MoMarkerAnnotation", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoMarkerAnnotation, ?> description = descriptionsMap.get(role);
        if(description == typeNameDescription) {
            super.typeName = (MoName) value;
        } else {
            logger.error("Role {} not found in MoMarkerAnnotation", role);
        }
    }

    public static Map<String, Description<MoMarkerAnnotation, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoMarkerAnnotation(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoMarkerAnnotation moMarkerAnnotation) {
            return super.typeName.isSame(moMarkerAnnotation.typeName);
        }
        return false;
    }
}
