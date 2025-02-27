package repair.dsl.kirin.map.code.node;

import repair.ast.code.MoModifier;
import repair.dsl.kirin.UnSupportException;

public class Modifier extends DSLNode {
    @Override
    public String prettyPrint() {
        throw new UnSupportException("Modifier");
    }

    public String getConditionStr(MoModifier modifier) {
        return switch (modifier.getModifierKind()) {
            case PUBLIC -> "isPublic";
            case PROTECTED -> "isProtected";
            case PRIVATE -> "isPrivate";
            case STATIC -> "isStatic";
            case ABSTRACT -> "isAbstract";
            case FINAL -> "isFinal";
            case NATIVE -> "isNative";
            case SYNCHRONIZED -> "isSynchronized";
            case TRANSIENT -> "isTransient";
            case VOLATILE -> "isVolatile";
            case STRICTFP -> "";
            case DEFAULT -> "isDefault";
        };
    }
}
