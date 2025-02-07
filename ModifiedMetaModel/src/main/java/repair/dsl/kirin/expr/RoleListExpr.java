package repair.dsl.kirin.expr;

import repair.dsl.kirin.alias.Alias;
import repair.dsl.kirin.map.code.role.DSLRole;

import java.util.List;

public class RoleListExpr implements Lhs{
    private final Alias alias;
    private final List<DSLRole> rolePaths;

    public RoleListExpr(Alias alias, List<DSLRole> rolePaths) {
        this.alias = alias;
        this.rolePaths = rolePaths;
    }

    @Override
    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        sb.append(alias.getAliasKey());
        for (DSLRole rolePath : rolePaths) {
            sb.append(".").append(rolePath.prettyPrint());
        }
        return sb.toString();
    }
}
