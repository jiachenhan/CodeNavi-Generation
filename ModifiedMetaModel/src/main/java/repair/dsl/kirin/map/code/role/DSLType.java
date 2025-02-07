package repair.dsl.kirin.map.code.role;

public class DSLType extends DSLRole {
    public DSLType() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "type";
    }
}
