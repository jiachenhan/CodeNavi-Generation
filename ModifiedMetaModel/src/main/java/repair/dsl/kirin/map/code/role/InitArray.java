package repair.dsl.kirin.map.code.role;

public class InitArray extends DSLRole {
    public InitArray() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "initArray";
    }
}
