package repair.dsl.kirin.map.code.role;

public class SwitchSelector extends DSLRole {
    public SwitchSelector() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "selector";
    }
}
