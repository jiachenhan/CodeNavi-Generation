package repair.dsl.kirin.expr;

public class DSLNodeExpr implements Rhs {
    private final String keyword;

    public DSLNodeExpr(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }

    @Override
    public String prettyPrint() {
        return keyword;
    }
}
