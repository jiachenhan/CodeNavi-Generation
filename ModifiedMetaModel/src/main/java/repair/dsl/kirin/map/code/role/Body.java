package repair.dsl.kirin.map.code.role;

public class Body extends DSLRole {
    public Body() {
        super(RoleAction.Body);
    }

    @Override
    public String prettyPrint() {
        return "body";
    }
}
