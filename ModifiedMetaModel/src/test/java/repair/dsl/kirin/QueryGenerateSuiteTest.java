package repair.dsl.kirin;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.ast.MoNode;
import repair.ast.parser.NodeParser;
import repair.dsl.kirin.query.Query;
import repair.pattern.Pattern;
import repair.pattern.abstraction.RandomAbstractor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static repair.common.JDTUtils.genASTFromFile;
import static repair.common.JDTUtils.getMethodDeclarations;

public class QueryGenerateSuiteTest {
    private static final Logger logger = LoggerFactory.getLogger(QueryGenerateSuiteTest.class);
    private static Path baseTestInputPath;

    @BeforeClass
    public static void setUpClass() {
        baseTestInputPath = Path.of("08example/code");
        logger.info("baseTestInputPath: {}", baseTestInputPath);
    }

    @Test
    public void ExampleTest() {
        Path testInputPath = baseTestInputPath.resolve("correct.java");
        List<Pattern> patterns = extractSinglePatterns(testInputPath);
        for (Pattern pattern : patterns) {
            Query query = QueryGenerator.getInstance().generate(pattern);
            System.out.println(query.prettyPrint());
        }
    }

    private List<Pattern> extractSinglePatterns(Path codePath) {
        ArrayList<Pattern> result = new ArrayList<>();
        CompilationUnit compilationUnit = genASTFromFile(codePath);
        for (MethodDeclaration methodDeclaration : getMethodDeclarations(compilationUnit)) {
            NodeParser nodeParser = new NodeParser(codePath, compilationUnit);
            MoNode moNode = nodeParser.process(methodDeclaration);
            Pattern pattern = new Pattern(moNode);
//            new RandomAbstractor().doAbstraction(pattern);
            result.add(pattern);
        }
        return result;
    }
}
