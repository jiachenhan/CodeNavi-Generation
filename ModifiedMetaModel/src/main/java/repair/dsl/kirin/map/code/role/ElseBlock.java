package repair.dsl.kirin.map.code.role;

public class ElseBlock extends DSLRole {
    public ElseBlock() {
        super(RoleAction.Body);
    }

    @Override
    public String prettyPrint() {
        return "elseBlock";
    }
}
