package repair.dsl.kirin.map.code.role;

public class Dimensions extends DSLRole {
    public Dimensions() {
        super(RoleAction.Collection);
    }

    @Override
    public String prettyPrint() {
        return "dimensions";
    }
}
