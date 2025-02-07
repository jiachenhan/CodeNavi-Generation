package repair.dsl.kirin.map.code.role;

public class ArrayInitElements extends DSLRole {
    public ArrayInitElements() {
        super(RoleAction.Collection);
    }

    @Override
    public String prettyPrint() {
        return "elements";
    }
}
