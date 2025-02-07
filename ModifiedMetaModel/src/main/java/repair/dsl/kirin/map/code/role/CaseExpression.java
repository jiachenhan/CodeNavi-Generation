package repair.dsl.kirin.map.code.role;

public class CaseExpression extends DSLRole {
    public CaseExpression() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "caseExpression";
    }
}
