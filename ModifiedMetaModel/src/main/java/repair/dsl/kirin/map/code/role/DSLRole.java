package repair.dsl.kirin.map.code.role;

import repair.dsl.kirin.Printable;

public abstract class DSLRole implements Printable {
    protected final RoleAction roleAction;

    public DSLRole(RoleAction roleAction) {
        this.roleAction = roleAction;
    }

    public RoleAction getRoleAction() {
        return roleAction;
    }
}
