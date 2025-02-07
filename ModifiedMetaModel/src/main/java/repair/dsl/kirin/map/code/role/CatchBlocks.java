package repair.dsl.kirin.map.code.role;

public class CatchBlocks extends DSLRole {
    public CatchBlocks() {
        super(RoleAction.Body);
    }

    @Override
    public String prettyPrint() {
        return "catchBlocks";
    }
}
