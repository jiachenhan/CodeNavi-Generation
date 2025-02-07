package repair.dsl.kirin.map.code.role;

public class Operand extends DSLRole {
    public Operand() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "operand";
    }
}
