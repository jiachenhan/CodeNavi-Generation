package repair.modify.builder;

import com.github.gumtreediff.tree.Tree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.tree.Type;
import repair.ast.MoNode;

import static com.github.gumtreediff.tree.TypeSet.type;

public class MoGumtreeBuilder {

    private final TreeContext treeContext = new TreeContext();

    public Tree getTree(MoNode node) {
        Type type = type("root");
        final Tree root = treeContext.createTree(type, "");
        new MoGumtreeScanner(treeContext, root).scan(node);
        return root;
    }

}
