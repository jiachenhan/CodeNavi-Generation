package repair.dsl.kirin.map.code.role;

public class DSLCondition extends DSLRole {
    public DSLCondition() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "condition";
    }
}
