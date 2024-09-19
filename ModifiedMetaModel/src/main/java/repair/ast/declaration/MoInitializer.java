package repair.ast.declaration;

import org.eclipse.jdt.core.dom.Initializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeList;
import repair.ast.MoNodeType;
import repair.ast.code.MoExtendedModifier;
import repair.ast.code.MoJavadoc;
import repair.ast.code.MoModifier;
import repair.ast.code.statement.MoBlock;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.*;

public class MoInitializer extends MoBodyDeclaration {
    private static final Logger logger = LoggerFactory.getLogger(MoInitializer.class);
    @Serial
    private static final long serialVersionUID = 8354269523571116218L;

    private final static Description<MoInitializer, MoJavadoc> javadocDescription =
            new Description<>(ChildType.CHILD, MoInitializer.class, MoJavadoc.class,
                    "javadoc", false);

    private final static Description<MoInitializer, MoExtendedModifier> modifiersDescription =
            new Description<>(ChildType.CHILDLIST, MoInitializer.class, MoExtendedModifier.class,
                    "modifiers", true);

    private final static Description<MoInitializer, MoBlock> bodyDescription =
            new Description<>(ChildType.CHILD, MoInitializer.class, MoBlock.class,
                    "body", true);

    private final static Map<String, Description<MoInitializer, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("javadoc", javadocDescription),
            Map.entry("modifiers", modifiersDescription),
            Map.entry("body", bodyDescription)
    );

    @RoleDescriptor(type = ChildType.CHILD, role = "body", mandatory = true)
    private MoBlock body;

    public MoInitializer(String fileName, int startLine, int endLine, Initializer initializer) {
        super(fileName, startLine, endLine, initializer);
        moNodeType = MoNodeType.TYPEInitializer;
        super.modifiers = new MoNodeList<>(this, modifiersDescription);
    }

    public MoBlock getBody() {
        return body;
    }

    public void setBody(MoBlock body) {
        this.body = body;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoInitializer(this);
    }

    @Override
    public List<MoNode> getChildren() {
        List<MoNode> children = new ArrayList<>();
        if(super.javadoc != null) {
            children.add(super.javadoc);
        }
        super.modifiers.forEach(modifier -> children.add(((MoNode) modifier)));
        children.add(body);
        return Collections.unmodifiableList(children);
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoInitializer, ?> description = descriptionsMap.get(role);
        if(description == javadocDescription) {
            return super.javadoc;
        } else if(description == modifiersDescription) {
            return super.modifiers;
        } else if(description == bodyDescription) {
            return body;
        } else {
            logger.error("Role {} not found in MoInitializer", role);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoInitializer, ?> description = descriptionsMap.get(role);
        if(description == javadocDescription) {
            super.javadoc = (MoJavadoc) value;
        } else if(description == modifiersDescription) {
            super.modifiers.clear();
            super.modifiers.addAll((MoNodeList<MoExtendedModifier>) value);
        } else if(description == bodyDescription) {
            body = (MoBlock) value;
        } else {
            logger.error("Role {} not found in MoInitializer", role);
        }
    }

    public static Map<String, Description<MoInitializer, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        return new MoInitializer(getFileName(), getStartLine(), getEndLine(), null);
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoInitializer otherInitializer) {
            boolean match;
            if(super.javadoc == null) {
                match = otherInitializer.javadoc == null;
            } else {
                match = super.javadoc.isSame(otherInitializer.javadoc);
            }
            match = match && MoExtendedModifier.sameList(super.modifiers, otherInitializer.modifiers);
            match = match && body.isSame(otherInitializer.body);
            return match;
        }
        return false;
    }
}
