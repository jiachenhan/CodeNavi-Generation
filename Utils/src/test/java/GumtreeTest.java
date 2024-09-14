import com.github.gumtreediff.actions.ChawatheScriptGenerator;
import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.Tree;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.fail;

public class GumtreeTest {
    private final Path datasetPath = Paths.get("E:/dataset/api/apache-API-cluster");


    /**
     * use maven dependency gen.jdt
     */
    @Test
    public void GenJdtTest() {
        String projectName = "archiva";
        String GroupName = "15";
        Path groupPath = datasetPath.resolve(projectName).resolve(GroupName);
        String casePattern = "1e1f7cdacd0118a5fb9a707871c7b7100b7f09d2--DefaultRepositoryGroupService-DefaultRepositoryGroupService--101-103_102-105";

        Path codeBeforePath = groupPath.resolve(casePattern).resolve("before.java");
        Path codeAfterPath = groupPath.resolve(casePattern).resolve("after.java");

        CompilationUnit beforeCompilationUnit = genASTFromFile(codeBeforePath);
        CompilationUnit afterCompilationUnit = genASTFromFile(codeAfterPath);

        Optional<MethodDeclaration> methodBefore = getOnlyMethodDeclaration(beforeCompilationUnit);
        Optional<MethodDeclaration> methodAfter = getOnlyMethodDeclaration(afterCompilationUnit);

        if(methodBefore.isEmpty() || methodAfter.isEmpty()) {
            fail("MethodDeclaration is not present");
        }

        JdtTreeGenerator generator = new JdtTreeGenerator();
        try {
            Tree beforeTree = generator.generateFrom().file(codeBeforePath).getRoot();
            Tree afterTree = generator.generateFrom().file(codeAfterPath).getRoot();

            // 使用 GumTree 匹配器匹配两个树
            Matcher defaultMatcher = Matchers.getInstance().getMatcher(); // retrieves the default matcher
            MappingStore mappings = defaultMatcher.match(beforeTree, afterTree); // computes the mappings between the trees

            EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator(); // instantiates the simplified Chawathe script generator
            EditScript actions = editScriptGenerator.computeActions(mappings); // computes the edit script

            System.out.println(actions);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            code = Files.readString(srcPath);
        } catch (IOException e) {
        }

        return (CompilationUnit) compile(code, srcPath.toString());
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
