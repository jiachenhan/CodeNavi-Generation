package repair.pattern;

import com.github.gumtreediff.actions.model.Action;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import repair.ast.MoNode;
import repair.ast.visitor.FlattenScanner;
import repair.modify.builder.GumtreeMetaConstant;
import repair.modify.diff.DiffComparator;
import repair.modify.diff.operations.Operation;
import repair.pattern.attr.Attribute;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pattern implements Serializable {
    @Serial
    private static final long serialVersionUID = -5977154810927770357L;
    /**
     * patternBefore0 -> patternAfter0 作为最开始初始化的pattern，需要有其他的before -> after树与之匹配
     */
    private final MoNode patternBefore0;
    private final MoNode patternAfter0;
    private final DiffComparator diffComparator;
    private List<Operation<? extends Action>> allOperations;
    private final BidiMap<MoNode, MoNode> beforeToAfterMap = new DualHashBidiMap<>();

    public Pattern(MoNode patternBefore0, MoNode patternAfter0) {
        this.patternBefore0 = patternBefore0;
        this.patternAfter0 = patternAfter0;
        diffComparator = new DiffComparator();
        diffComparator.computeBeforeAfterMatch(patternBefore0, patternAfter0);
        this.allOperations = diffComparator.getAllOperations();

        diffComparator.getMappings().asSet().forEach(mapping -> {
            MoNode beforeNode = (MoNode) mapping.first.getMetadata(GumtreeMetaConstant.MO_NODE_KEY);
            MoNode afterNode = (MoNode) mapping.second.getMetadata(GumtreeMetaConstant.MO_NODE_KEY);
            beforeToAfterMap.put(beforeNode, afterNode);
        });

        initAttributes();
    }

    private final Map<MoNode, Map<Class<? extends Attribute<?>>, Attribute<?>>> nodeToAttributes = new HashMap<>();

    /**
     * 初始化patternBefore0中的节点的属性
     */
    public void initAttributes() {
        for (MoNode beforeNode : new FlattenScanner().flatten(patternBefore0)) {
            Map<Class<? extends Attribute<?>>, Attribute<?>> attributes = AttributeFactory.createAttributes(beforeNode);
            nodeToAttributes.put(beforeNode, attributes);
        }
    }

    public Map<MoNode, Map<Class<? extends Attribute<?>>, Attribute<?>>> getNodeToAttributes() {
        return nodeToAttributes;
    }

    /*
    * 考虑patternBefore中的哪些节点，节点中的哪些属性，以及节点之间的关系
    *
    * 展开每个节点，计算节点的属性
    *
    * 如何思考cluster的问题？
    * patternBefore -> patternAfter 作为最开始初始化的pattern，需要有其他的before -> after树与之匹配
    * 1. 需要满足匹配的父子关系
    * 2. 需要匹配对应的Operation，Operation必须是相同的
    *
    * 在这种情况下，匹配成功但是属性不完全一致的节点需要对属性进行抽象（抽象到父关系上e.g. InfixExpression/PostExpression -> Expression，或者完全抽象掉改属性）
    * children的LocationInParent应该完全的一致
    * 聚类后可以进行下一步的抽象
    *
    * */


    public DiffComparator getDiffComparator() {
        return diffComparator;
    }

    public List<Operation<? extends Action>> getAllOperations() {
        return allOperations;
    }

    public MoNode getPatternBefore0() {
        return patternBefore0;
    }

    public MoNode getPatternAfter0() {
        return patternAfter0;
    }
}
