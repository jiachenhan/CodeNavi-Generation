package repair.dsl.kirin.expr;

public class NameAttrExpr implements Lhs{

    private final RoleListExpr roleListExpr;
    private final String postfix;

    public NameAttrExpr(RoleListExpr roleListExpr, String postfix) {
        this.roleListExpr = roleListExpr;
        this.postfix = postfix;
    }

    @Override
    public String prettyPrint() {
        return roleListExpr.prettyPrint() + "." + postfix;
    }
}
