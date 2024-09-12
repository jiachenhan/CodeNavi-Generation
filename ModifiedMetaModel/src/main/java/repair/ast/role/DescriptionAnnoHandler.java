package repair.ast.role;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

@Deprecated
public class DescriptionAnnoHandler {
    private final static Logger logger = LoggerFactory.getLogger(DescriptionAnnoHandler.class);

    private static Optional<Field> getFieldByRole(MoNode moParent, String role) {
        Class<?> clazz = moParent.getClass();
        // 该节点类及其父类的所有字段
        for (Field field : clazz.getFields()) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(RoleDescriptor.class)) {
                RoleDescriptor roleDescriptor = field.getAnnotation(RoleDescriptor.class);
                if (roleDescriptor.role().equals(role)) {
                    return Optional.of(field);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 根据role获取父节点中的字段
     * @param moParent 父节点
     * @param role 角色
     * @return 字段
     * @param <T> 返回字段的类型
     */
    @SuppressWarnings("unchecked")
    public static <T> T getFieldValueByRole(MoNode moParent, String role) {
        Optional<Field> field = getFieldByRole(moParent, role);
        if (field.isPresent()) {
            try {
                return (T) field.get().get(moParent);
            } catch (IllegalAccessException e) {
                logger.error("Failed to get role value", e);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static void addFieldByRole(MoNode moParent, String role, MoNode value) {
        Optional<Field> field = getFieldByRole(moParent, role);
        Optional<RoleDescriptor> roleDescription = getRoleDescription(moParent, role);
        if (field.isPresent() && roleDescription.isPresent()) {
            try {
                RoleDescriptor description = roleDescription.get();
                Object fieldValue = field.get().get(moParent);
                if(description.type() == ChildType.CHILDLIST) {
                    addToList((List<MoNode>) fieldValue, value);
                } else if (description.type() == ChildType.CHILD) {
                    field.get().set(moParent, value);
                } else {
                    // try to set Simple Child
                }
            } catch (IllegalAccessException e) {
                logger.error("Failed to set role value", e);
            }
        }
    }

    /**
     * helper function to add value to list, used in updateFieldByRole
     */
    private static <T extends MoNode> void addToList(List<T> list, T value) {
        list.add(value);
    }

    public static Optional<RoleDescriptor> getRoleDescription(MoNode moParent, String role) {
        Optional<Field> field = getFieldByRole(moParent, role);
        return field.map(value -> value.getAnnotation(RoleDescriptor.class));
    }

    public static RoleDescriptor getDescriptionOfChild(MoNode moParent, MoNode moChild) {
        if(moParent == null || moChild == null || moChild.getParent() != moParent) {
            return null;
        }

        Class<?> clazz = moParent.getClass();

        // 遍历父节点的所有字段
        for (Field field : clazz.getFields()) {
            field.setAccessible(true);

            // 检查字段是否有Role注解
            if (field.isAnnotationPresent(RoleDescriptor.class)) {
                try {
                    // 获取字段的值，检查是否与传入的子节点相同
                    Object value = field.get(moParent);
                    if (value == moChild) {
                        return field.getAnnotation(RoleDescriptor.class);  // 返回Role的值
                    }
                } catch (IllegalAccessException e) {
                    logger.error("Failed to get role value", e);
                }
            }
        }
        return null; // 如果没有找到匹配的字段，返回null
    }
}
