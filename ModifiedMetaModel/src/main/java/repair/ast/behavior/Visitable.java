package repair.ast.behavior;

import repair.ast.visitor.Visitor;

public interface Visitable {
    void accept(Visitor visitor);
}
