package repair.ast.parser;

import repair.ast.MoNode;

import java.util.HashMap;
import java.util.Map;

public class VScope {
    private final VScope parent;
    private Map<String, MoNode> varDefines = new HashMap<>();
    private Map<String, MoNode> varUsed = new HashMap<>();

    public VScope(VScope parent) {
        this.parent = parent;
    }

    public void addDefine(String name, MoNode moNode) {
        if (name == null || moNode == null) return;
        varDefines.put(name, moNode);
    }

    public MoNode getDefines(String name) {
        if(name == null) {
            return null;
        }
        return gDefines(name);
    }

    private MoNode gDefines(String name) {
        MoNode moNode = varDefines.get(name);
        if (moNode == null && parent != null) {
            return parent.gDefines(name);
        }
        return moNode;
    }

    public void addUse(String name, MoNode moNode) {
        if(name == null || moNode == null) return;
        varUsed.put(name, moNode);
    }

    public MoNode getUse(String name) {
        if(name == null) {
            return null;
        }
        return gUse(name);
    }

    private MoNode gUse(String name) {
        MoNode moNode = varUsed.get(name);
        if(name == null && parent != null) {
            return gUse(name);
        }
        return moNode;
    }
}
