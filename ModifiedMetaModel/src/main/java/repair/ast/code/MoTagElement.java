package repair.ast.code;

import org.eclipse.jdt.core.dom.TagElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.*;

public class MoTagElement extends MoNode implements MoDocElement {
    private static final Logger logger = LoggerFactory.getLogger(MoTagElement.class);
    @Serial
    private static final long serialVersionUID = -6924172468196528514L;

    private final static Description<MoTagElement, String> tagNameDescription =
            new Description<>(ChildType.SIMPLE, MoTagElement.class, String.class,
                    "tagName", false);

    private final static Description<MoTagElement, MoDocElement> fragmentsDescription =
            new Description<>(ChildType.CHILDLIST, MoTagElement.class, MoDocElement.class,
                    "fragments", true);

    private final static Map<String, Description<MoTagElement, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("tagName", tagNameDescription),
            Map.entry("fragments", fragmentsDescription)
    );

    @RoleDescriptor(type = ChildType.SIMPLE, role = "tagName", mandatory = false)
    private String tagName;
    @RoleDescriptor(type = ChildType.CHILDLIST, role = "fragments", mandatory = true)
    private final MoNodeList<MoDocElement> docFragments;

    public MoTagElement(String fileName, int startLine, int endLine, TagElement tagName) {
        super(fileName, startLine, endLine, tagName);
        moNodeType = MoNodeType.TYPETagElement;
        docFragments = new MoNodeList<>(this, fragmentsDescription);
    }


    public void addDocFragment(MoDocElement docFragment) {
        docFragments.add(docFragment);
    }

    public Optional<String> getTagName() {
        return Optional.ofNullable(tagName);
    }

    public List<MoDocElement> getDocFragments() {
        return docFragments;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoTagElement(this);
    }

    @Override
    public List<MoNode> getChildren() {
        List<MoNode> children = new ArrayList<>();
        docFragments.forEach(docFragment -> children.add(((MoNode) docFragment)));
        return Collections.unmodifiableList(children);
    }

    @Override
    public boolean isLeaf() {
        return docFragments.isEmpty();
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoTagElement, ?> description = descriptionsMap.get(role);
        if(description == tagNameDescription) {
            return tagName;
        } else if(description == fragmentsDescription) {
            return docFragments;
        } else {
            logger.error("Role {} not found in MoTagElement", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoTagElement, ?> description = descriptionsMap.get(role);
        if(description == tagNameDescription) {
            this.tagName = (String) value;
        } else if(description == fragmentsDescription) {
            this.docFragments.clear();
            this.docFragments.addAll((List<MoDocElement>) value);
        } else {
            logger.error("Role {} not found in MoTagElement", role);
        }
    }

    public static Map<String, Description<MoTagElement, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoTagElement clone = new MoTagElement(getFileName(), getStartLine(), getEndLine(), null);
        getTagName().ifPresent(tagName -> clone.setStructuralProperty("tagName", tagName));
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoTagElement moTagElement) {
            boolean match = Objects.equals(tagName, moTagElement.tagName);
            match = match && MoDocElement.sameList(docFragments, moTagElement.docFragments);
            return match;
        }
        return false;
    }
}
