package repair.common;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.FileUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JDTUtils {

    private static final Logger logger = LoggerFactory.getLogger(JDTUtils.class);

    /**
     *  only consider the top method declarations (ignore anonymous class)
     */
    public static Optional<MethodDeclaration> getDeclaration(CompilationUnit unit, MethodSignature method) {
        if (method == null || unit == null) return Optional.empty();
        final List<MethodDeclaration> declarations = new ArrayList<>();
        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration m) {
                if (method.sameSignature(m)) {
                    declarations.add(m);
                }
                return false;
            }
        });
        if (declarations.size() > 1) {
            logger.warn("Multiple method declarations found for method: " + method.getName());
        }
        return declarations.isEmpty() ? Optional.empty() : Optional.of(declarations.get(0));
    }

    public static Optional<MethodDeclaration> getOnlyMethodDeclaration(CompilationUnit unit) {
        if (unit == null) return Optional.empty();
        final List<MethodDeclaration> declarations = new ArrayList<>(1);
        unit.accept(new ASTVisitor() {
            public boolean visit(MethodDeclaration m) {
                declarations.add(m);
                return false;
            }
        });
        if (declarations.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(declarations.get(0));
    }

    public static CompilationUnit genASTFromFile(Path srcPath) {
        String code = "";
        try {
            String encoding = FileUtils.detectCharset(srcPath);
            code = Files.readString(srcPath, Charset.forName(encoding));
        } catch (IOException e) {
            logger.error("Failed to read file: " + srcPath, e);
        }

        return compile(code, srcPath.toString());
    }

    public static CompilationUnit genAST(String code, Path codePath) {
        return compile(code, codePath.toString());
    }


    public static ASTNode genASTFromSourceWithType(String icu, int type, String filePath, String srcPath) {
        return genASTFromSourceWithType(icu, JavaCore.VERSION_1_7, AST.JLS8, type, filePath, srcPath);
    }

    public static CompilationUnit compile(String code, String srcPath) {
        if (code == null || code.isEmpty()) return null;
        return (CompilationUnit) genASTFromSourceWithType(code, ASTParser.K_COMPILATION_UNIT, srcPath, null);
    }

    public synchronized static ASTNode genASTFromSourceWithType(String icu, String jversion, int astLevel, int type,
                                                                String filePath, String srcPath) {
        if(icu == null || icu.isEmpty()) return null;
        ASTParser astParser = ASTParser.newParser(astLevel);
        Map<String, String> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(jversion, options);
        astParser.setCompilerOptions(options);
        astParser.setSource(icu.toCharArray());
        astParser.setKind(type);
        astParser.setResolveBindings(true);
        srcPath = srcPath == null ? "" : srcPath;
        filePath = filePath == null ? "" : filePath;
        astParser.setEnvironment(getClassPath(), new String[] {srcPath}, null, true);
        astParser.setUnitName(filePath);
        astParser.setBindingsRecovery(true);
        try{
            return astParser.createAST(null);
        }catch(Exception e) {
            return null;
        }
    }

    private static String[] getClassPath() {
        String property = System.getProperty("java.class.path", ".");
        return property.split(File.pathSeparator);
    }

}
