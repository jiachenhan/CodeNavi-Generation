package repair.dsl.kirin.condition;

import repair.dsl.kirin.expr.Lhs;
import repair.dsl.kirin.expr.Rhs;

public class BinaryCondition extends Condition {
    public enum Predicate {
        EQ("=="),
        IS("is"),
        CONTAIN("contain"),
        NOT_IN("notIn"),
        MATCH("match");

        private final String key;
        private Predicate(String key) {
            this.key = key;
        }
        public String getKey() {
            return key;
        }
    }

    private final Predicate predicate;
    private final Lhs lhs;
    private final Rhs rhs;

    public BinaryCondition(Predicate predicate, Lhs lhs, Rhs rhs) {
        this.predicate = predicate;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public String prettyPrint() {
        return lhs.prettyPrint() + " " + predicate.getKey() + " " + rhs.prettyPrint();
    }
}
