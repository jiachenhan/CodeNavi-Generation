package repair.dsl.kirin.condition;

public class NotCondition extends Condition {
    private static final String predicate = "not";

    private final Condition innerCondition;

    public NotCondition(Condition innerCondition) {
        this.innerCondition = innerCondition;
    }

    @Override
    public String prettyPrint() {
        return predicate + "(" + innerCondition.prettyPrint() + ")";
    }
}
