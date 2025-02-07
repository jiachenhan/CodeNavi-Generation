package repair.dsl.kirin.map.code.role;

public class ForEachIterable extends DSLRole {
    public ForEachIterable() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "iterable";
    }
}
