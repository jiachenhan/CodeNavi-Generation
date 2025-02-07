package repair.dsl.kirin.map.code.role;

public class AnnoMembers extends DSLRole {
    public AnnoMembers() {
        super(RoleAction.Body);
    }

    @Override
    public String prettyPrint() {
        return "annoMembers";
    }
}
