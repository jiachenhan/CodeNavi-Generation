package repair.ast.analysis;

import repair.ast.code.type.MoType;
import repair.ast.declaration.MoVariableDeclaration;

import java.io.Serial;
import java.io.Serializable;

public record VariableDef(MoVariableDeclaration variable, MoType variableType, int scopeStart,
                          int scopeEnd) implements Serializable {
    @Serial
    private static final long serialVersionUID = -5682842343546774403L;
}
