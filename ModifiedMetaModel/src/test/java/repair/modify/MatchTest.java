package repair.modify;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Before;
import org.junit.Test;
import repair.apply.ApplyModification;
import repair.apply.match.MatchInstance;
import repair.apply.match.Matcher;
import repair.ast.MoNode;
import repair.ast.parser.NodeParser;
import repair.ast.visitor.DeepCopyScanner;
import repair.modify.diff.DiffComparator;
import repair.pattern.Pattern;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static repair.common.JDTUtils.genASTFromFile;
import static repair.common.JDTUtils.getOnlyMethodDeclaration;

public class MatchTest {
    private final Path datasetPath = Paths.get("E:/dataset/api/apache-API-cluster");
    private final String projectName = "opennlp";
    private final String GroupName = "2";
    private final Path groupPath = datasetPath.resolve(projectName).resolve(GroupName);
    private final String caseName = "186ecf924cf13cc982bf9ca15c9487f473e4a9c8--POSModel-POSModel--250-252_256-258";

    private Pattern pattern;

    private MoNode moNode;


    @Test
    public void MatchTest() {
        System.out.println(pattern);
        System.out.println(moNode);

        List<MatchInstance> matchInstances = Matcher.match(pattern, moNode);
        System.out.println("Match instances: " + matchInstances.size());
    }

    @Test
    public void RepairTest() {
        List<MatchInstance> matchInstances = Matcher.match(pattern, moNode);
        System.out.println("Match instances: " + matchInstances.size());

        ApplyModification applyModification = new ApplyModification(pattern, moNode, matchInstances.get(0));
        applyModification.apply();
        System.out.println(applyModification.getRight());
        assertEquals("not same", pattern.getPatternAfter0().toString(), applyModification.getRight().toString());
    }


    @Before
    public void buildPattern() {
        Path patternBeforePath = groupPath.resolve(caseName).resolve("before.java");
        Path patternAfterPath = groupPath.resolve(caseName).resolve("after.java");

        CompilationUnit beforeCompilationUnit = genASTFromFile(patternBeforePath);
        CompilationUnit afterCompilationUnit = genASTFromFile(patternAfterPath);

        Optional<MethodDeclaration> methodBefore = getOnlyMethodDeclaration(beforeCompilationUnit);
        Optional<MethodDeclaration> methodAfter = getOnlyMethodDeclaration(afterCompilationUnit);

        if(methodBefore.isEmpty() || methodAfter.isEmpty()) {
            fail("MethodDeclaration is not present");
        }

        NodeParser beforeParser = new NodeParser(patternBeforePath.toString(), beforeCompilationUnit);
        NodeParser afterParser = new NodeParser(patternAfterPath.toString(), afterCompilationUnit);

        MoNode moMethodBefore = beforeParser.process(methodBefore.get());
        MoNode moMethodAfter = afterParser.process(methodAfter.get());

        pattern = new Pattern(moMethodBefore, moMethodAfter, DiffComparator.Mode.MOVE_MODE);
        DeepCopyScanner deepCopyScanner = new DeepCopyScanner(moMethodBefore);
        moNode = deepCopyScanner.getCopy();
    }

}
