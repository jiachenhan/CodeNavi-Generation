package repair.dsl.kirin.map.code.role;

public class DSLRhs extends DSLRole {
    public DSLRhs() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "rhs";
    }
}
