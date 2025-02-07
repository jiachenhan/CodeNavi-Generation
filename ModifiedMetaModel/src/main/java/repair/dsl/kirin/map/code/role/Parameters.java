package repair.dsl.kirin.map.code.role;

public class Parameters extends DSLRole {
    public Parameters() {
        super(RoleAction.Collection);
    }

    @Override
    public String prettyPrint() {
        return "parameters";
    }
}
