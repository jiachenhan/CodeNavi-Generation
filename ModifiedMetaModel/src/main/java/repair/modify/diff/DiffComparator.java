package repair.modify.diff;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.actions.model.Action;
import com.github.gumtreediff.matchers.CompositeMatchers;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.Tree;
import repair.ast.MoNode;
import repair.modify.builder.MoGumtreeBuilder;
import repair.modify.diff.operations.Operation;

import java.util.ArrayList;
import java.util.List;

public class DiffComparator {
    private final List<Operation<? extends Action>> allOperations = new ArrayList<>();

    private final Matcher defaultMatcher;
    private final EditScriptGenerator editScriptGenerator;
    private MappingStore mappings;

    private Tree beforeTree;
    private Tree afterTree;

    public DiffComparator() {
        defaultMatcher = new CompositeMatchers.SimpleGumtree();
        editScriptGenerator = new SimplifiedChawatheScriptGenerator();
    }

    private void buildTrees(MoNode beforeNode, MoNode afterNode) {
        beforeTree = new MoGumtreeBuilder().getTree(beforeNode);
        afterTree = new MoGumtreeBuilder().getTree(afterNode);
    }

    public void computeBeforeAfterMatch(MoNode beforeNode, MoNode afterNode) {
        buildTrees(beforeNode, afterNode);
        mappings = defaultMatcher.match(beforeTree, afterTree); // computes the mappings between the trees
        EditScript actions = editScriptGenerator.computeActions(mappings); // computes the edit script

        actions.asList().stream()
                .map(action -> Operation.createOperation(action, mappings))
                .forEach(allOperations::add);
    }

    public MappingStore getMappings() {
        return mappings;
    }

    public List<Operation<? extends Action>> getAllOperations() {
        return allOperations;
    }
}
