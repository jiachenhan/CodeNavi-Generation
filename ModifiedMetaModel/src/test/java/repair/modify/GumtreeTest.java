package repair.modify;

import com.github.gumtreediff.actions.EditScript;
import com.github.gumtreediff.actions.EditScriptGenerator;
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator;
import com.github.gumtreediff.matchers.MappingStore;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.matchers.Matchers;
import com.github.gumtreediff.tree.Tree;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.Assert.fail;
import static repair.common.JDTUtils.genASTFromFile;
import static repair.common.JDTUtils.getOnlyMethodDeclaration;

public class GumtreeTest {
    private final Path datasetPath = Paths.get("E:/dataset/api/apache-API-cluster");


    /**
     * use maven dependency gen.jdt
     */
    @Test
    public void GenJdtTest() {
        String projectName = "apex-core";
        String GroupName = "2";
        Path groupPath = datasetPath.resolve(projectName).resolve(GroupName);
        String casePattern = "b2b3d12b03d868f6a1023ad80ad88d651596d3fd--Controller-Controller--42-43_43-44";

        Path codeBeforePath = groupPath.resolve(casePattern).resolve("before.java");
        Path codeAfterPath = groupPath.resolve(casePattern).resolve("after.java");

        CompilationUnit beforeCompilationUnit = genASTFromFile(codeBeforePath);
        CompilationUnit afterCompilationUnit = genASTFromFile(codeAfterPath);

        Optional<MethodDeclaration> methodBefore = getOnlyMethodDeclaration(beforeCompilationUnit);
        Optional<MethodDeclaration> methodAfter = getOnlyMethodDeclaration(afterCompilationUnit);

        if(methodBefore.isEmpty() || methodAfter.isEmpty()) {
            fail("MethodDeclaration is not present");
        }

//        JdtTreeGenerator generator = new JdtTreeGenerator();
//        try {
//            Tree beforeTree = generator.generateFrom().file(codeBeforePath).getRoot();
//            Tree afterTree = generator.generateFrom().file(codeAfterPath).getRoot();
//
//            // 使用 GumTree 匹配器匹配两个树
//            Matcher defaultMatcher = Matchers.getInstance().getMatcher(); // retrieves the default matcher
//            MappingStore mappings = defaultMatcher.match(beforeTree, afterTree); // computes the mappings between the trees
//
//            EditScriptGenerator editScriptGenerator = new SimplifiedChawatheScriptGenerator(); // instantiates the simplified Chawathe script generator
//            EditScript actions = editScriptGenerator.computeActions(mappings); // computes the edit script
//
//            System.out.println(actions);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }

}
