package repair.dsl.kirin.map.code.role;

public class ForEachVariable extends DSLRole {
    public ForEachVariable() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "variable";
    }
}
