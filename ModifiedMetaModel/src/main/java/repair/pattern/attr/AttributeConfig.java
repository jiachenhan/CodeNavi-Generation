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

    // the default attribute config as the following

    private final static AttributeConfig attributeConfig;

    static {
        attributeConfig = new AttributeConfig();
        attributeConfig.addAttribute("LocationSubTypeAttribute", LocationSubTypeAttribute.class, 1.0, LocationSubTypeAttribute::new);
        attributeConfig.addAttribute("NameAttribute", NameAttribute.class, 1.0, NameAttribute::new);
        attributeConfig.addAttribute("ExprTypeAttribute", ExprTypeAttribute.class, 1.0, ExprTypeAttribute::new);
        attributeConfig.addAttribute("MoTypeAttribute", MoTypeAttribute.class, 0.5, MoTypeAttribute::new);
        attributeConfig.addAttribute("TokenAttribute", TokenAttribute.class, 0.5, TokenAttribute::new);
    }

    public static AttributeConfig getAttributeConfig() {
        return attributeConfig;
    }
}
