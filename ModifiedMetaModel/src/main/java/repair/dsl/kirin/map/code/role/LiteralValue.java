package repair.dsl.kirin.map.code.role;

public class LiteralValue extends DSLRole {
    public LiteralValue() {
        super(RoleAction.Simple);
    }

    @Override
    public String prettyPrint() {
        return "value";
    }
}
