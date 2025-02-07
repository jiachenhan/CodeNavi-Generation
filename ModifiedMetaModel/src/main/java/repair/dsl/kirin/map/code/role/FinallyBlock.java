package repair.dsl.kirin.map.code.role;

public class FinallyBlock extends DSLRole {
    public FinallyBlock() {
        super(RoleAction.Body);
    }

    @Override
    public String prettyPrint() {
        return "finallyBlock";
    }
}
