package repair.dsl.kirin.map.code.role;

public class Lock extends DSLRole{
    public Lock() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "lock";
    }
}
