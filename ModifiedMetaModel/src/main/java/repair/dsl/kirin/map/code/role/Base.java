package repair.dsl.kirin.map.code.role;

public class Base extends DSLRole {
    public Base() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "base";
    }
}
