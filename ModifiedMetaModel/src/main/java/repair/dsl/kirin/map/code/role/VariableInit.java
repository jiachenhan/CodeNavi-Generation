package repair.dsl.kirin.map.code.role;

public class VariableInit extends DSLRole {
    public VariableInit() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "initializer";
    }
}
