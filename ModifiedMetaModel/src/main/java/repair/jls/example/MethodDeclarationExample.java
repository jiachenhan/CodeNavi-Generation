package repair.jls.example;

import java.io.IOException;

public abstract class MethodDeclarationExample {
    public static void main(String[] args) throws Exception, IOException {
        System.out.println("Hello, World!");
    }

    public abstract int[] method1(int a);

    public void method2(int a, int b) {

    }
}

class OuterClass {
    class InnerClass {
        void exampleMethod(OuterClass.InnerClass this) {
            // 方法体
        }
    }
}