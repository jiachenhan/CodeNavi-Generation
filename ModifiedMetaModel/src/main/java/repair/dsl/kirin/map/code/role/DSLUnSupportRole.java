package repair.dsl.kirin.map.code.role;

import repair.dsl.kirin.UnSupportException;

public class DSLUnSupportRole extends DSLRole {
    public DSLUnSupportRole() {
        super(RoleAction.Interrupt);
    }

    @Override
    public String prettyPrint() {
        throw new UnSupportException("UnSupportRole");
    }
}
