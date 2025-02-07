package repair.dsl.kirin.map.code.role;

public class Arguments extends DSLRole {
    public Arguments() {
        super(RoleAction.Collection);
    }

    @Override
    public String prettyPrint() {
        return "arguments";
    }
}
