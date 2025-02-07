package repair.dsl.kirin.map.code.role;

public class AnnoValue extends DSLRole {
    public AnnoValue() {
        super(RoleAction.Child);
    }

    @Override
    public String prettyPrint() {
        return "annoValue";
    }
}
