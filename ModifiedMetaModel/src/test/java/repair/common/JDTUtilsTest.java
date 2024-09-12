package repair.common;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.Assert.*;
import static repair.common.JDTUtils.genASTFromFile;
import static repair.common.JDTUtils.getOnlyMethodDeclaration;

public class JDTUtilsTest {
    private final Path datasetPath = Paths.get("E:/dataset/api/apache-API-cluster");

    @Test
    public void genASTFromFileTest() {
        String projectName = "apex-core";
        String GroupName = "2";
        Path groupPath = datasetPath.resolve(projectName).resolve(GroupName);
//        String caseBuggy = "c33b3fe6e337afe22008cdfe728404efd742e183--AdapterWrapperNode-AdapterWrapperNode--170-171_170-171";
//        String casePattern = "33b3fe6e337afe22008cdfe728404efd742e183--AdapterWrapperNode-AdapterWrapperNode--170-171_170-171";
        String caseBuggy = "b2b3d12b03d868f6a1023ad80ad88d651596d3fd--Controller-Controller--36-37_37-38";
        String casePattern = "b2b3d12b03d868f6a1023ad80ad88d651596d3fd--Controller-Controller--42-43_43-44";

        Path patternBeforePath = groupPath.resolve(casePattern).resolve("before.java");
        CompilationUnit compilationUnit = genASTFromFile(patternBeforePath);

        if(compilationUnit == null) {
            fail("CompilationUnit is null");
        }

        Optional<MethodDeclaration> onlyMethodDeclaration = getOnlyMethodDeclaration(compilationUnit);
        if(onlyMethodDeclaration.isEmpty()) {
            fail("MethodDeclaration is not present");
        }

        System.out.println(compilationUnit.getLineNumber(onlyMethodDeclaration.get().getStartPosition()));
    }

}