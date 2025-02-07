package repair.dsl.kirin.map.code.role;

public class Generics extends DSLRole {
    public Generics() {
        super(RoleAction.Collection);
    }

    @Override
    public String prettyPrint() {
        return "generics";
    }
}
