package repair.dsl.kirin.map.code.role;

public class BaseType extends DSLRole {
    public BaseType() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "baseType";
    }
}
