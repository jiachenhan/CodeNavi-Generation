package repair.dsl.kirin.condition;

import repair.dsl.kirin.Printable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class UnionCondition extends Condition {
    public enum Predicate {
        OR("or"),
        AND("and");

        private final String key;

        Predicate(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    private final Predicate predicate;
    private final List<Condition> innerConditions;

    public UnionCondition(Predicate predicate) {
        this.predicate = predicate;
        this.innerConditions = new ArrayList<>();
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public void addInnerCondition(Condition innerCondition) {
        boolean isDuplicate = innerConditions.stream()
                .map(Printable::prettyPrint)
                .anyMatch(s -> s.equals(innerCondition.prettyPrint()));
        if (!isDuplicate) {
            innerConditions.add(innerCondition);
        }
    }

    @Override
    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        sb.append(predicate.getKey()).append("(").append("\n");
        Iterator<Condition> iterator = innerConditions.iterator();
        while (iterator.hasNext()) {
            Condition condition = iterator.next();
            sb.append(condition.prettyPrint());
            if (iterator.hasNext()) {
                sb.append(",").append("\n");
            }
        }
        sb.append("\n").append(")");
        return sb.toString();
    }
}
