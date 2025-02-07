package repair.dsl.kirin.expr;

public class StringExpr implements Rhs {
    private final String keyword;
    public StringExpr(String keyword) {
        this.keyword = keyword;
    }

    public String getKeyword() {
        return keyword;
    }

    @Override
    public String prettyPrint() {
        return "\"" + keyword + "\"";
    }
}
