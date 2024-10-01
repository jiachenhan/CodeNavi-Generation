package repair.ast.analysis;

import repair.ast.code.expression.MoName;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;


/**
 * This class is used to manage the global and local variables in the code.
 * 先不考虑全局变量
 */

public class IdentifierManager implements Serializable {
    @Serial
    private static final long serialVersionUID = 8149762077847382317L;
    private final Set<VariableDef> globalVars = new HashSet<>();
    private final Set<VariableDef> localVars = new HashSet<>();


    /**
     *  This class is used to represent the use of an identifier in the code.
     *  it might be a variable or a method
     */
    private final Map<String, List<MoName>> identifierUseMap = new HashMap<>();


    public void addGlobalVar(VariableDef var) {
        globalVars.add(var);
    }

    public void addLocalVar(VariableDef var) {
        localVars.add(var);
    }

    public void addIdentifierUse(String identifier, MoName name) {
        List<MoName> nameList = identifierUseMap.computeIfAbsent(identifier, k -> new ArrayList<>());
        nameList.add(name);
    }

    public Map<String, List<MoName>> getIdentifierUseMap() {
        return identifierUseMap;
    }

    public Set<VariableDef> getGlobalVars() {
        return globalVars;
    }

    public Set<VariableDef> getLocalVars() {
        return localVars;
    }

}
