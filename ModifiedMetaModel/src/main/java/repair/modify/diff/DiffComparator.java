package repair.modify.diff;

import com.github.gumtreediff.actions.*;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.actions.model.Delete;
import com.github.gumtreediff.actions.model.TreeDelete;
import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;
import repair.ast.MoNode;
import repair.modify.builder.MoGumtreeBuilder;
import repair.modify.diff.operations.DeleteOperation;
import repair.modify.diff.operations.Operation;
import repair.modify.diff.operations.TreeDeleteOperation;

import java.util.ArrayList;
import java.util.List;

public class DiffComparator {
    private final List<Operation<? extends Action>> allOperations = new ArrayList<>();

    private final Mode mode;

    private final Matcher defaultMatcher;
    private final EditScriptGenerator editScriptGenerator;
    private MappingStore mappings;

    private Tree beforeTree;
    private Tree afterTree;

    public enum Mode {
        MOVE_MODE, NO_MOVE_MODE
    }

    public DiffComparator(Mode mode) {
        this.mode = mode;
        defaultMatcher = new CompositeMatchers.SimpleGumtreeStable();
        if(mode == Mode.MOVE_MODE)
            editScriptGenerator = new MoChawatheScriptGenerator();
        else if(mode == Mode.NO_MOVE_MODE)
            editScriptGenerator = new InsertDeleteChawatheScriptGenerator();
        else
            throw new IllegalArgumentException("Invalid mode");
    }

    private void buildTrees(MoNode beforeNode, MoNode afterNode) {
        beforeTree = new MoGumtreeBuilder().getTree(beforeNode);
        afterTree = new MoGumtreeBuilder().getTree(afterNode);
    }

    public void computeBeforeAfterMatch(MoNode beforeNode, MoNode afterNode) {
        buildTrees(beforeNode, afterNode);
        MappingStore oriMapping = defaultMatcher.match(beforeTree, afterTree); // computes the mappings between the trees
        EditScript actions = editScriptGenerator.computeActions(oriMapping); // computes the edit script
        if(this.mode == Mode.MOVE_MODE) {
            mappings = ((MoChawatheScriptGenerator) editScriptGenerator).getMappings();
        } else {
            mappings = oriMapping;
        }

        actions.asList().stream()
                .map(action -> Operation.createOperation(action, mappings))
//                .sorted((a1, a2) -> {
//                    // 将 Delete 和 TreeDelete 类型排在前面
//                    if (a1 instanceof DeleteOperation || a1 instanceof TreeDeleteOperation) {
//                        return -1;
//                    } else if (a2 instanceof DeleteOperation || a2 instanceof TreeDeleteOperation) {
//                        return 1;
//                    } else {
//                        return 0; // 保持其他顺序不变
//                    }
//                })
                .forEach(allOperations::add);
    }

    public MappingStore getMappings() {
        return mappings;
    }

    public List<Operation<? extends Action>> getAllOperations() {
        return allOperations;
    }
}
