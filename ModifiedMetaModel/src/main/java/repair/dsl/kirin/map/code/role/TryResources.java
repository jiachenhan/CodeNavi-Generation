package repair.dsl.kirin.map.code.role;

public class TryResources extends DSLRole {
    public TryResources() {
        super(RoleAction.Body);
    }

    @Override
    public String prettyPrint() {
        return "resources";
    }
}
