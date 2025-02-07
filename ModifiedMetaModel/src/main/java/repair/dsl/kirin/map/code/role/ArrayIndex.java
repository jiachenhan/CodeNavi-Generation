package repair.dsl.kirin.map.code.role;

public class ArrayIndex extends DSLRole {
    public ArrayIndex() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "arrayIndex";
    }
}
