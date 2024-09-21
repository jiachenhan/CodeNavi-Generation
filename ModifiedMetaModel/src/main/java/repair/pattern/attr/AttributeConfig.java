package repair.pattern.attr;

import repair.ast.MoNode;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class AttributeConfig {
    private static final Map<String, Class<? extends Attribute<?>>> registeredAttrs = new HashMap<>();
    private static final Map<String, Function<MoNode, Attribute<?>>> registeredAttrConstructors = new HashMap<>();

    // 属性权重，相加为1， 每个属性range [-1] [0, 1]
    public static final Map<Class<? extends Attribute<?>>, Double> attrToWeight = new HashMap<>();

    public void addAttribute(String name, Class<? extends Attribute<?>> attrClass, double weight, Function<MoNode, Attribute<?>> constructor) {
        attrToWeight.put(attrClass, weight);
        registeredAttrs.put(name, attrClass);
        registeredAttrConstructors.put(name, constructor);
    }

    public Map<Class<? extends Attribute<?>>, Double> getAttrToWeight() {
        return attrToWeight;
    }

    public Map<String, Class<? extends Attribute<?>>> getRegisteredAttrs() {
        return registeredAttrs;
    }

    public Map<String, Function<MoNode, Attribute<?>>> getRegisteredAttrConstructors() {
        return registeredAttrConstructors;
    }
}
