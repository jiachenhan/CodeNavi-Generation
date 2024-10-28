package repair.apply.builder;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;
import repair.ast.MoNode;
import repair.ast.parser.NodeParser;
import repair.apply.diff.DiffComparator;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.Assert.*;
import static repair.common.JDTUtils.genASTFromFile;
import static repair.common.JDTUtils.getOnlyMethodDeclaration;

public class MoGumtreeBuilderTest {
    private final Path datasetPath = Paths.get("E:/dataset/api/apache-API-cluster");

    @Test
    public void GenGumtreeTest() {
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

        NodeParser beforeParser = new NodeParser(codeBeforePath, beforeCompilationUnit);
        NodeParser afterParser = new NodeParser(codeAfterPath, afterCompilationUnit);

        MoNode moMethodBefore = beforeParser.process(methodBefore.get());
        MoNode moMethodAfter = afterParser.process(methodAfter.get());

        DiffComparator diffComparator = new DiffComparator(DiffComparator.Mode.MOVE_MODE);
        diffComparator.computeBeforeAfterMatch(moMethodBefore, moMethodAfter);
        System.out.println(diffComparator.getAllOperations());

    }

}