package repair.dsl.kirin.map.code;

import repair.dsl.kirin.map.code.node.DSLNode;
import repair.dsl.kirin.map.code.role.DSLRole;

import java.lang.reflect.Constructor;

public class KeyWordFactory {
    public static <T extends DSLNode> T createNodeInstance(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create node instance", e);
        }
    }

    public static <T extends DSLRole> T createRoleInstance(Class<T> clazz) {
        try {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create role instance", e);
        }
    }
}
