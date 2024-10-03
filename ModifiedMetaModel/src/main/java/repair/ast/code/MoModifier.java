package repair.ast.code;

import org.eclipse.jdt.core.dom.Modifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.MoNodeType;
import repair.ast.code.type.MoPrimitiveType;
import repair.ast.declaration.MoFieldDeclaration;
import repair.ast.declaration.MoVariableDeclarationFragment;
import repair.ast.role.ChildType;
import repair.ast.role.Description;
import repair.ast.role.RoleDescriptor;
import repair.ast.visitor.Visitor;

import java.io.Serial;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MoModifier extends MoNode implements MoExtendedModifier {
    private static final Logger logger = LoggerFactory.getLogger(MoModifier.class);
    @Serial
    private static final long serialVersionUID = 2431190446542845810L;

    private final static Description<MoModifier, MoModifier.ModifierKind> keywordDescription =
            new Description<>(ChildType.SIMPLE, MoModifier.class, MoModifier.ModifierKind.class,
                    "keyword", true);

    private final static Map<String, Description<MoModifier, ?>> descriptionsMap = Map.ofEntries(
            Map.entry("keyword", keywordDescription)
    );

    @RoleDescriptor(type = ChildType.SIMPLE, role = "keyword", mandatory = true)
    private MoModifier.ModifierKind kind;


    public enum ModifierKind{
        PUBLIC,
        PROTECTED,
        PRIVATE,
        STATIC,
        ABSTRACT,
        FINAL,
        NATIVE,
        SYNCHRONIZED,
        TRANSIENT,
        VOLATILE,
        STRICTFP,
        DEFAULT;

        private final String keyword;
        ModifierKind(){
            this.keyword = name().toLowerCase();
        }

        public static MoModifier.ModifierKind fromCode(String value) {
            for (MoModifier.ModifierKind modifierKind : MoModifier.ModifierKind.values()) {
                if (modifierKind.keyword.equals(value)) {
                    return modifierKind;
                }
            }
            throw new IllegalArgumentException("No enum constant for kind: " + value);
        }

        @Override
        public String toString(){
            return keyword;
        }

    }

    public MoModifier(Path fileName, int startLine, int endLine, Modifier modifier) {
        super(fileName, startLine, endLine, modifier);
        moNodeType = MoNodeType.TYPEModifier;
    }

    public ModifierKind getModifierKind() {
        return kind;
    }

    //    @Override
//    public boolean same(MoNode other) {
//        if(other instanceof MoModifier modifier){
//            return kind == modifier.kind;
//        }
//        return false;
//    }



    @Override
    public boolean isSame(MoNode other) {
        if(other instanceof MoModifier modifier){
            return this.kind.equals(modifier.kind);
        }
        return false;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitMoModifier(this);
    }

    @Override
    public boolean isModifier() {
        return true;
    }

    @Override
    public boolean isAnnotation() {
        return false;
    }

    @Override
    public List<MoNode> getChildren() {
        return List.of();
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public Object getStructuralProperty(String role) {
        Description<MoModifier, ?> description = descriptionsMap.get(role);
        if(description == keywordDescription) {
            return kind;
        } else {
            logger.error("Role {} not found in MoModifier", role);
            return null;
        }
    }

    @Override
    public void setStructuralProperty(String role, Object value) {
        Description<MoModifier, ?> description = descriptionsMap.get(role);
        if(description == keywordDescription) {
            kind = ModifierKind.fromCode((String) value);
        } else {
            logger.error("Role {} not found in MoModifier", role);
        }
    }

    public static Map<String, Description<MoModifier, ?>> getDescriptionsMap() {
        return descriptionsMap;
    }

    @Override
    public Description<? extends MoNode, ?> getDescription(String role) {
        return descriptionsMap.get(role);
    }

    @Override
    public MoNode shallowClone() {
        MoModifier clone = new MoModifier(getFileName(), getStartLine(), getEndLine(), null);
        clone.setStructuralProperty("keyword", this.getModifierKind().toString());
        return clone;
    }

}
