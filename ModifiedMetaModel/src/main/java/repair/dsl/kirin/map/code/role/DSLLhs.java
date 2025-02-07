package repair.dsl.kirin.map.code.role;

public class DSLLhs extends DSLRole {
    public DSLLhs() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "lhs";
    }
}
