package repair.dsl.kirin.map.code.role;

import repair.dsl.kirin.UnSupportException;

public class SkipRole extends DSLRole {
    public SkipRole() {
        super(RoleAction.Skip);
    }

    @Override
    public String prettyPrint() {
        throw new UnSupportException("SkipRole");
    }
}
