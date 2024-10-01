package repair.ast.code.context;

import repair.ast.MoNode;

import java.io.Serial;
import java.io.Serializable;

public class Context implements Serializable {
    @Serial
    private static final long serialVersionUID = -1115551854536480984L;
    private MoNode dataDependency;
    public void setDataDependency(MoNode dependency) {
        this.dataDependency = dependency;
    }
    /**
     * get data dependency
     *
     * @return : data dependent node, can be {@code null}
     */
    public MoNode getDataDependency() {
        return dataDependency;
    }

    protected MoNode controlDependency;

    /**
     * current variable is used by {@code Node} {@code preUseChain} used previously
     * NOTE: not null for variables only (e.g., Name, FieldAcc, and AryAcc etc.)
     */
    private MoNode preUseChain;
    /**
     * current variable will be used by {@code Node} {@code nextUseChain} used next
     * NOTE: not null for variables only (e.g., Name, FieldAcc, and AryAcc etc.)
     */
    private MoNode nextUseChain;


    public MoNode getControlDependency() {
        return controlDependency;
    }

    public void setControlDependency(MoNode controlDependency) {
        this.controlDependency = controlDependency;
    }

    public MoNode getPreUseChain() {
        return preUseChain;
    }

    public void setPreUseChain(MoNode preUseChain) {
        this.preUseChain = preUseChain;
    }

    public MoNode getNextUseChain() {
        return nextUseChain;
    }

    public void setNextUseChain(MoNode nextUseChain) {
        this.nextUseChain = nextUseChain;
    }
}
