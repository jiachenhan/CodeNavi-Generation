package repair.dsl.kirin.expr;

import repair.dsl.kirin.alias.Alias;

public class AliasExpr implements Lhs, Rhs{
    private final Alias alias;

    public AliasExpr(Alias alias) {
        this.alias = alias;
    }

    @Override
    public String prettyPrint() {
        return alias.getAliasKey();
    }
}
