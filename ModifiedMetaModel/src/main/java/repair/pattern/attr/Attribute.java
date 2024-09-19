package repair.pattern.attr;

import repair.ast.MoNode;
import repair.ast.visitor.FlattenScanner;
import repair.pattern.AttributeFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 描述某个节点的某种属性
 * @param <T> 属性值的类型
 */
public abstract class Attribute<T> {
    /**
     * 只在pattern中考虑
     */
    protected MoNode node;

    public Attribute(MoNode node){
        this.node = node;
    }

    protected boolean considered;
    protected T value;

    public void setConsidered(boolean considered){
        this.considered = considered;
    }

    public boolean isConsidered(){
        return considered;
    }
    public T getValue(){
        return value;
    }

    // for hard constraint, if the attribute is unMatched, return -1
    public abstract double similarity(Attribute<?> other);

    public static Map<MoNode, Map<Class<? extends Attribute<?>>, Attribute<?>>> computeAttributes(MoNode left) {
        Map<MoNode, Map<Class<? extends Attribute<?>>, Attribute<?>>> nodeToAttributes = new HashMap<>();
        for (MoNode leftNode : new FlattenScanner().flatten(left)) {
            Map<Class<? extends Attribute<?>>, Attribute<?>> attributes = AttributeFactory.createAttributes(leftNode);
            nodeToAttributes.put(leftNode, attributes);
        }
        return nodeToAttributes;
    }

}
