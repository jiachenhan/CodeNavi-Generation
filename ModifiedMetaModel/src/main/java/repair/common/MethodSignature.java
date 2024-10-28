package repair.common;

import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class MethodSignature {
    private static final Logger logger = LoggerFactory.getLogger(MethodSignature.class);
    private final String returnType;
    private final String methodName;
    private final List<String> argumentTypes;

    public MethodSignature(String returnType, String methodName, List<String> argumentTypes) {
        this.returnType = returnType;
        this.methodName = methodName;
        this.argumentTypes = argumentTypes;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getName() {
        return methodName;
    }

    public List<String> getArgTypes() {
        return argumentTypes;
    }

    @Override
    public String toString() {
        return  returnType + " " + methodName + argumentTypes.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MethodSignature other) {
            return Objects.equals(this.returnType, other.returnType) &&
                    this.methodName.equals(other.methodName) &&
                    this.argumentTypes.equals(other.argumentTypes);
        }
        return false;
    }

    public boolean sameSignature(MethodDeclaration method) {
        String retType = method.getReturnType2() == null ? null : method.getReturnType2().toString();
        String name = method.getName().getIdentifier();
        if (Objects.equals(this.returnType, retType) && methodName.equals(name)) {
            @SuppressWarnings("unchecked")
            List<SingleVariableDeclaration> args = (List<SingleVariableDeclaration>) method.parameters();
            if (args.size() != argumentTypes.size()) {
                return false;
            }
            for (int i = 0; i < argumentTypes.size(); i++) {
                if (!argumentTypes.get(i).equals(args.get(i).getType().toString())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public static MethodSignature parseFunctionSignature(String signature) {
        String decoratedCode = "public class TempClass {\n" +
                "    " + signature + "{}" + "\n" +
                "}";

        ASTParser parser = ASTParser.newParser(AST.JLS19);
        parser.setSource(decoratedCode.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        List<MethodSignature> methodSignatures = new ArrayList<>();
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                @SuppressWarnings("unchecked")
                List<String> argumentTypes = ((List<SingleVariableDeclaration>)node.parameters()).stream()
                        .map(parameter -> parameter.getType().toString()).toList();
                MethodSignature methodSignature = new MethodSignature(
                        node.getReturnType2() == null ? null : node.getReturnType2().toString(),
                        node.getName().getIdentifier(),
                        argumentTypes
                );
                methodSignatures.add(methodSignature);
                return false; // Do not visit deeper
            }
        });

        if (methodSignatures.size() != 1) {
            logger.error("MethodSignature parse error: " + signature);
            return null;
        }
        return methodSignatures.get(0);
    }
}
