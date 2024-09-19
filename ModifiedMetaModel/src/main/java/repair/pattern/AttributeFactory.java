package repair.pattern;

import com.github.gumtreediff.utils.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.pattern.attr.Attribute;
import repair.pattern.attr.LocationSubTypeAttribute;
import repair.pattern.attr.MoTypeAttribute;
import repair.pattern.attr.TokenAttribute;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AttributeFactory {
    private final static Logger logger = LoggerFactory.getLogger(AttributeFactory.class);
    private static final Map<String, Class<? extends Attribute<?>>> registeredAttrs = new HashMap<>();
    private static final Map<String, Function<MoNode, Attribute<?>>> registeredAttrConstructors = new HashMap<>();
    public static final Map<Class<? extends Attribute<?>>, Double> attrToWeight = new HashMap<>();

    static {
        attrToWeight.put(LocationSubTypeAttribute.class, 1.0); // weight is not used for hard constraints

        attrToWeight.put(MoTypeAttribute.class, 0.5);
        attrToWeight.put(TokenAttribute.class, 0.5);
    }

    static {
        // hard constraints
        registeredAttrs.put("LocationSubTypeAttr", LocationSubTypeAttribute.class);

        registeredAttrs.put("TokenAttr", TokenAttribute.class);
        registeredAttrs.put("MoTypeAttr", MoTypeAttribute.class);
    }

    static {
        registeredAttrConstructors.put("LocationSubTypeAttr", LocationSubTypeAttribute::new);

        registeredAttrConstructors.put("TokenAttr", TokenAttribute::new);
        registeredAttrConstructors.put("MoTypeAttr", MoTypeAttribute::new);
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
