package repair.ast;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Before;
import org.junit.Test;
import repair.ast.parser.NodeParser;
import repair.ast.visitor.DeepCopyScanner;
import repair.ast.visitor.FlattenScanner;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static repair.common.JDTUtils.genASTFromFile;
import static repair.common.JDTUtils.getOnlyMethodDeclaration;

public class MoNodeTest {

    private final Path datasetPath = Paths.get("E:/dataset/api/apache-API-cluster");
    private final String projectName = "opennlp";
    private final String GroupName = "2";
    private final Path groupPath = datasetPath.resolve(projectName).resolve(GroupName);
    private final String caseName = "186ecf924cf13cc982bf9ca15c9487f473e4a9c8--POSModel-POSModel--250-252_256-258";
    private final String fileName = "before.java";


    private MoNode moNode;

    @Test
    public void ParseNodeTest() {
        System.out.println(moNode.toString());
    }

    @Test
    public void FlattenScannerTest() {
        List<MoNode> flattened = new FlattenScanner().flatten(moNode);
        System.out.println("Flattened nodes: " + flattened.size());
        for (MoNode node : flattened) {
            System.out.println(node);
        }
    }

    @Test
    public void TokenizeScannerTest() {
        List<String> tokens = moNode.tokens();
        System.out.println("Tokens: " + tokens.size());

        for (String token : tokens) {
            System.out.println(token);
        }
    }



    @Before
    public void buildMoNode() {
        Path patternBeforePath = groupPath.resolve(caseName).resolve(fileName);
        CompilationUnit compilationUnit = genASTFromFile(patternBeforePath);

        Optional<MethodDeclaration> onlyMethodDeclaration = getOnlyMethodDeclaration(compilationUnit);
        if(onlyMethodDeclaration.isEmpty()) {
            fail("MethodDeclaration is not present");
        }

        NodeParser parser = new NodeParser(patternBeforePath, compilationUnit);
        moNode = parser.process(onlyMethodDeclaration.get());
    }

}