package repair.dsl.kirin.map.code.role;

public class ThenBlock extends DSLRole {
    public ThenBlock() {
        super(RoleAction.Body);
    }

    @Override
    public String prettyPrint() {
        return "thenBlock";
    }
}
