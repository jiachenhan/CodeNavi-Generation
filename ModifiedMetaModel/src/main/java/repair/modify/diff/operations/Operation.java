package repair.modify.diff.operations;

import com.github.gumtreediff.actions.model.*;
import com.github.gumtreediff.matchers.MappingStore;

public abstract class Operation<T extends Action> {
    protected final T action;
    public Operation(T action) {
        this.action = action;
    }
    public T getAction() {
        return action;
    }

    public static Operation<? extends Action> createOperation(Action action, MappingStore mappings) {
        if (action instanceof Insert insert) {
            return new InsertOperation(insert);
        } else if (action instanceof Update update) {
            return new UpdateOperation(update);
        } else if (action instanceof Move move) {
            return new MoveOperation(move, mappings);
        } else if (action instanceof Delete delete) {
            return new DeleteOperation(delete);
        } else if(action instanceof TreeDelete treeDelete) {
            return new TreeDeleteOperation(treeDelete);
        } else if (action instanceof TreeInsert treeInsert) {
            return new TreeInsertOperation(treeInsert);
        } else {
            throw new IllegalArgumentException("Unknown action type: " + action.getClass().getName());
        }
    }

}
