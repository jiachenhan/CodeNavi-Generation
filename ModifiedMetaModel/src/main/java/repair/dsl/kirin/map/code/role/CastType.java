package repair.dsl.kirin.map.code.role;

public class CastType extends DSLRole {
    public CastType() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "castType";
    }
}
