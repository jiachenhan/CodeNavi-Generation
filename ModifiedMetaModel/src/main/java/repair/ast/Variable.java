/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package repair.ast;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;


public class Variable implements Serializable {
    @Serial
    private static final long serialVersionUID = 3850565694419541106L;
    private String name;
    private String type;

    public Variable(String name, String type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Variable var)) {
            return false;
        }
        // currently consider name only
        // if type is needed, update here
        return Objects.equals(name, var.name);
//                && Utils.safeStringEqual(_type, var._type);
    }
}
