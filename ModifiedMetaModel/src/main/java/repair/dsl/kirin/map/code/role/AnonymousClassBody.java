package repair.dsl.kirin.map.code.role;

public class AnonymousClassBody extends DSLRole {
    public AnonymousClassBody() {
        super(RoleAction.Body);
    }

    @Override
    public String prettyPrint() {
        return "anonymousClassBody";
    }
}
