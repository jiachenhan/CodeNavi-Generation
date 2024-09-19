package repair.ast.code;

import org.eclipse.jdt.core.dom.Javadoc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.declaration.MoFieldDeclaration;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MoJavadoc extends MoComment {
    private static final Logger logger = LoggerFactory.getLogger(MoJavadoc.class);
    @Serial
    private static final long serialVersionUID = 4011228051753761053L;

    private final static Description<MoJavadoc, MoTagElement> tagsDescription =
            new Description<>(ChildType.CHILDLIST, MoJavadoc.class, MoTagElement.class,
                    "tags", true);

    private final static Map<String, Description<MoJavadoc, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("tags", tagsDescription)
    );

    @RoleDescriptor(type = ChildType.CHILDLIST, role = "tags", mandatory = true)
    private final MoNodeList<MoTagElement> tagElements;

    public MoJavadoc(String fileName, int startLine, int endLine, Javadoc javadoc) {
        super(fileName, startLine, endLine, javadoc);
        moNodeType = MoNodeType.TYPEJavadoc;
        tagElements = new MoNodeList<>(this, tagsDescription);
    }

    public void addTagElement(MoTagElement tagElement) {
        tagElements.add(tagElement);
    }

    public List<MoTagElement> getTagElements() {
        return tagElements;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoJavadoc(this);
    }

    @Override
    public List<MoNode> getChildren() {
        return Collections.unmodifiableList(tagElements);
    }

    @Override
    public boolean isLeaf() {
        return tagElements.isEmpty();
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoJavadoc, ?> description = descriptionsMap.get(role);
        if(description == tagsDescription) {
            return tagElements;
        } else {
            logger.error("Role {} not found in MoJavadoc", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoJavadoc, ?> description = descriptionsMap.get(role);
        if(description == tagsDescription) {
            tagElements.clear();
            tagElements.addAll((List<MoTagElement>) value);
        } else {
            logger.error("Role {} not found in MoJavadoc", role);
        }
    }

    public static Map<String, Description<MoJavadoc, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoJavadoc(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoJavadoc otherJavadoc) {
            return MoNodeList.sameList(tagElements, otherJavadoc.tagElements);
        }
        return false;
    }
}
