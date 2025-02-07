package repair.dsl.kirin.map.code.role;

public class PrimitiveTypeCode extends DSLRole {
    public PrimitiveTypeCode() {
        super(RoleAction.Simple);
    }

    @Override
    public String prettyPrint() {
        return "name";
    }
}
