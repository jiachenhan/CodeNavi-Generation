package repair.dsl.kirin.map.code.role;

public class ReturnValue extends DSLRole {
    public ReturnValue() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "returnValue";
    }
}
