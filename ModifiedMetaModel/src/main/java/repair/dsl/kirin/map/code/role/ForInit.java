package repair.dsl.kirin.map.code.role;

public class ForInit extends DSLRole {
    public ForInit() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "initialization";
    }
}
