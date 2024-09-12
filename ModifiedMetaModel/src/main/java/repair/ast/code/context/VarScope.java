/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package repair.ast.code.context;


import repair.ast.Variable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// todo: 通过Binding修改
public class VarScope {

    private Set<Variable> globalVars;
    private Map<Variable, Set<LineRange>> localVars;
    private transient Set<Variable> newVars;
    private boolean disable = false;

    public VarScope() {
        globalVars = new HashSet<>();
        localVars = new HashMap<>();
    }

    public void setDisable(boolean disable) {
        this.disable = disable;
    }

    public void reset(Set<Variable> newVars) {
        this.newVars = newVars;
    }

    public void setGlobalVars(final Set<Variable> globalVar) {
        globalVars = globalVar;
    }

    public void addGlobalVar(Variable var) {
        globalVars.add(var);
    }

    public void addGlobalVar(final String name, final String type) {
        addGlobalVar(new Variable(name, type));
    }

    public void addLocalVar(Variable variable, LineRange lineRange) {
        Set<LineRange> ranges = localVars.get(variable);
        if (ranges == null) {
            ranges = new HashSet<>();
            localVars.put(variable, ranges);
        }
        ranges.add(lineRange);
    }

    public void addLocalVar(final String name, final String type, final int start, final int end) {
        Variable variable = new Variable(name, type);
        LineRange range = new LineRange(start, end);
        addLocalVar(variable, range);
    }

    public boolean canUse(final String name, final String type, final int line) {
        if (disable) return true;
        Variable variable = new Variable(name, type);
        Set<LineRange> ranges = localVars.get(variable);
        if (ranges != null) {
            for (LineRange r : ranges) {
                if (r.start() < line/*r.contains(line)*/) {
                    return true;
                }
            }
        }
        if (newVars != null) {
            if (newVars.contains(variable)) {
                return true;
            }
        }
        return globalVars.contains(variable);
    }

}

