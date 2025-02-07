package repair.dsl.kirin.map.code.role;

public class TryBlock extends DSLRole {
    public TryBlock() {
        super(RoleAction.Body);
    }

    @Override
    public String prettyPrint() {
        return "tryBlock";
    }
}
