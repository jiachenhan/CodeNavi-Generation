package repair.dsl.kirin.map.code.role;

public class Annotations extends DSLRole {
    public Annotations() {
        super(RoleAction.Body);
    }

    @Override
    public String prettyPrint() {
        return "annotations";
    }
}
