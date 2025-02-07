package repair.dsl.kirin.map.code.role;

public class ElseExpression extends DSLRole {
    public ElseExpression() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "elseExpression";
    }
}
