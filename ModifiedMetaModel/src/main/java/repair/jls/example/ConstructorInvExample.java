package repair.jls.example;

import java.util.ArrayList;
import java.util.List;

public class ConstructorInvExample<T> {
    T value;
    String valueStr;

    public ConstructorInvExample(String str, T value) {
        <T>this(value);
    }

    public ConstructorInvExample(T value) {
        // 带泛型参数的构造函数
        this.value = value;
        this.valueStr = value.toString();
    }
}