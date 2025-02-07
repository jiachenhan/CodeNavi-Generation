package repair.dsl.kirin.map.code.role;

public class ThenExpression extends DSLRole {
    public ThenExpression() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "thenExpression";
    }
}
