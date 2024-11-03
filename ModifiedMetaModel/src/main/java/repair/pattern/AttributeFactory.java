package repair.pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.pattern.abstraction.TermFrequencyAbstractor;
import repair.pattern.attr.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class AttributeFactory {
    private final static Logger logger = LoggerFactory.getLogger(AttributeFactory.class);
    private static final Map<String, Class<? extends Attribute<?>>> registeredAttrs = new HashMap<>();
    private static final Map<String, Function<MoNode, Attribute<?>>> registeredAttrConstructors = new HashMap<>();

    // 属性权重，相加为1， 每个属性range [-1] [0, 1]
    public static final Map<Class<? extends Attribute<?>>, Double> attrToWeight = new HashMap<>();

    // 抽象器属性配置
    private static final AttributeConfig config = AttributeConfig.getAttributeConfig();

    static {
        attrToWeight.putAll(config.getAttrToWeight());
        registeredAttrs.putAll(config.getRegisteredAttrs());
        registeredAttrConstructors.putAll(config.getRegisteredAttrConstructors());
    }

    public static Attribute<?> createAttr(String key, MoNode initArg) throws IllegalAccessException, InstantiationException {
        Function<MoNode, Attribute<?>> constructor = registeredAttrConstructors.get(key);
        if (constructor == null) {
            throw new IllegalArgumentException("No attr registered with key: " + key);
        }
        return constructor.apply(initArg);
    }

    public static Map<Class<? extends Attribute<?>>, Attribute<?>> createAttributes(MoNode node) {
        Map<Class<? extends Attribute<?>>, Attribute<?>> attributes = new HashMap<>();
        for (String key : registeredAttrs.keySet()) {
            try {
                Attribute<?> attr = createAttr(key, node);
                attributes.put(registeredAttrs.get(key), attr);
            } catch (IllegalAccessException | InstantiationException e) {
                logger.error("Error creating attribute: " + key, e);
            }
        }
        return attributes;
    }

}
