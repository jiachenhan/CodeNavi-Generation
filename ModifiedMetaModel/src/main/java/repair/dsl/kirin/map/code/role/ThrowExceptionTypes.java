package repair.dsl.kirin.map.code.role;

public class ThrowExceptionTypes extends DSLRole {
    public ThrowExceptionTypes() {
        super(RoleAction.Collection);
    }

    @Override
    public String prettyPrint() {
        return "exceptionTypes";
    }
}
