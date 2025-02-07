package repair.dsl.kirin.map.code.role;

public class ForIter extends DSLRole {
    public ForIter() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "iteration";
    }
}
