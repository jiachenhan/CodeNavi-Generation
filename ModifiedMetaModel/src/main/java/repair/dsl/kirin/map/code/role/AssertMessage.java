package repair.dsl.kirin.map.code.role;

public class AssertMessage extends DSLRole {
    public AssertMessage() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "message";
    }
}
