package repair.ast.code;

import org.eclipse.jdt.core.dom.TextElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.statement.MoVariableDeclarationStatement;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.util.Map;

public class MoTextElement extends MoNode implements MoDocElement{
    private static final Logger logger = LoggerFactory.getLogger(MoTextElement.class);
    @Serial
    private static final long serialVersionUID = -7538325106755614392L;

    private final static Description<MoTextElement, String> textDescription =
            new Description<>(ChildType.SIMPLE, MoTextElement.class, String.class,
                    "text", true);

    private final static Map<String, Description<MoTextElement, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("text", textDescription)
    );

    @RoleDescriptor(type = ChildType.SIMPLE, role = "text", mandatory = true)
    private String text;

    public MoTextElement(String fileName, int startLine, int endLine, TextElement textElement) {
        super(fileName, startLine, endLine, textElement);
        moNodeType = MoNodeType.TYPETextElement;
    }

    public String getText() {
        return text;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoTextElement(this);
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoTextElement, ?> description = descriptionsMap.get(role);
        if(description == textDescription) {
            return text;
        } else {
            logger.error("Role {} not found in MoTextElement", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoTextElement, ?> description = descriptionsMap.get(role);
        if(description == textDescription) {
            text = (String) value;
        } else {
            logger.error("Role {} not found in MoTextElement", role);
        }
    }

    public static Map<String, Description<MoTextElement, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoTextElement clone = new MoTextElement(getFileName(), getStartLine(), getEndLine(), null);
        clone.setStructuralProperty("text", getText());
        return clone;
    }

    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoTextElement moTextElement) {
            return text.equals(moTextElement.text);
        }
        return false;
    }
}
