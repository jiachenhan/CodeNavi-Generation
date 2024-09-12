package repair.common;

import org.eclipse.jdt.core.dom.*;
import org.junit.Test;
import org.junit.runner.OrderWith;

import java.util.List;

public class JDTDescirptorTest {
    @Test
    public void test() {
        // 代码解析部分
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setSource("public class Example { void method() { int x = 5; } }".toCharArray());
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        // 访问 AST 节点
        cu.accept(new ASTVisitor() {
//            @Override
//            public boolean visit(VariableDeclarationFragment node) {
//                // 获取当前字段的描述符
//                StructuralPropertyDescriptor descriptor = node.getLocationInParent();
//                if (descriptor instanceof ChildListPropertyDescriptor listPropertyDescriptor) {
//                    List<VariableDeclarationFragment> VDFs = (List<VariableDeclarationFragment>) node.getParent().getStructuralProperty(listPropertyDescriptor);
//
//                }
//                return super.visit(node);
//            }

            @Override
            public boolean visit(MethodDeclaration node) {
                // 获取当前字段的描述符
                StructuralPropertyDescriptor descriptor = node.getLocationInParent();
                if (descriptor instanceof ChildListPropertyDescriptor listPropertyDescriptor) {
                    List<VariableDeclarationFragment> VDFs = (List<VariableDeclarationFragment>) node.getParent().getStructuralProperty(listPropertyDescriptor);

                }
                return super.visit(node);
            }
        });
    }



}
