package repair.ast.visitor;

import repair.ast.MoNode;

import java.util.ArrayList;
import java.util.List;

public class FlattenScanner extends DeepScanner {
    private final List<MoNode> nodes = new ArrayList<>();

    public List<MoNode> flatten(MoNode node) {
        node.accept(this);
        return nodes;
    }

    @Override
    public void enter(MoNode node) {
        nodes.add(node);
    }
}
