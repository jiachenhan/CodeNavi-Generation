package repair.ast.visitor;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;
import repair.ast.MoNode;
import repair.ast.parser.NodeParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static repair.common.JDTUtils.genASTFromFile;
import static repair.common.JDTUtils.getOnlyMethodDeclaration;

public class CodePrinterTest {
    private final Path datasetPath = Paths.get("E:/dataset/api/apache-API-cluster");

    private final List<Path> excludedPaths = List.of(
            // over JLS8 limit (instanceof pattern)
            Path.of("E:\\dataset\\api\\apache-API-cluster\\opennlp\\2\\186ecf924cf13cc982bf9ca15c9487f473e4a9c8--POSModel-POSModel--250-252_256-258\\before.java"),
            Path.of("E:\\dataset\\api\\apache-API-cluster\\opennlp\\2\\186ecf924cf13cc982bf9ca15c9487f473e4a9c8--POSModel-POSModel--250-252_256-258\\after.java")
    );

    @Test
    public void testPrintAll() {
        try(Stream<Path> javaStream = Files.walk(datasetPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))) {
            javaStream.forEach(path -> {
                if(excludedPaths.contains(path)) {
                    return;
                }

                System.out.println("Processing: " + path);
                String OriginalCode = null;
                try {
                    OriginalCode = Files.readString(path);
                } catch (IOException e) {
                    System.out.println("Error reading origin file");
                    fail();
                }

                CompilationUnit compilationUnit = genASTFromFile(path);
                Optional<MethodDeclaration> onlyMethodDeclaration = getOnlyMethodDeclaration(compilationUnit);
                if(onlyMethodDeclaration.isEmpty()) {
                    fail("MethodDeclaration is not present");
                }

                NodeParser parser = new NodeParser(path.toString(), compilationUnit);
                MoNode moNode = parser.process(onlyMethodDeclaration.get());
                String afterParseCode = "class PlaceHold {" + moNode.toString() + "}";

                OriginalCode = clearAllSpaces(OriginalCode);
                afterParseCode = clearAllSpaces(afterParseCode);

                assertEquals("Code not equal in " + path.toString(), OriginalCode, afterParseCode);

            });
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testCopyAll() {
        try(Stream<Path> javaStream = Files.walk(datasetPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))) {
            javaStream.forEach(path -> {
                if(excludedPaths.contains(path)) {
                    return;
                }

                System.out.println("Processing: " + path);
                String OriginalCode = null;
                try {
                    OriginalCode = Files.readString(path);
                } catch (IOException e) {
                    System.out.println("Error reading origin file");
                    fail();
                }

                CompilationUnit compilationUnit = genASTFromFile(path);
                Optional<MethodDeclaration> onlyMethodDeclaration = getOnlyMethodDeclaration(compilationUnit);
                if(onlyMethodDeclaration.isEmpty()) {
                    fail("MethodDeclaration is not present");
                }

                NodeParser parser = new NodeParser(path.toString(), compilationUnit);
                MoNode moNode = parser.process(onlyMethodDeclaration.get());

                DeepCopyScanner deepCopyScanner = new DeepCopyScanner(moNode);
                MoNode copy = deepCopyScanner.getCopy();
                String afterCopyCode = "class PlaceHold {" + copy.toString() + "}";

                OriginalCode = clearAllSpaces(OriginalCode);
                afterCopyCode = clearAllSpaces(afterCopyCode);

                assertEquals("Code not equal in " + path.toString(), OriginalCode, afterCopyCode);

            });
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void singleCopyTest() {
        String projectName = "apex-core";
        String GroupName = "10";
        Path groupPath = datasetPath.resolve(projectName).resolve(GroupName);

        String casePattern = "e4d44e559376eb6203e19f186139334ad1b3f318--StramClient-StramClient--575-575_575-576";
        Path path = groupPath.resolve(casePattern).resolve("after.java");
        String OriginalCode = null;
        try {
            OriginalCode = Files.readString(path);
        } catch (IOException e) {
            System.out.println("Error reading origin file");
            fail();
        }

        CompilationUnit compilationUnit = genASTFromFile(path);

        Optional<MethodDeclaration> onlyMethodDeclaration = getOnlyMethodDeclaration(compilationUnit);
        if(onlyMethodDeclaration.isEmpty()) {
            fail("MethodDeclaration is not present");
        }

        NodeParser parser = new NodeParser(path.toString(), compilationUnit);
        MoNode moNode = parser.process(onlyMethodDeclaration.get());

        DeepCopyScanner deepCopyScanner = new DeepCopyScanner(moNode);
        MoNode copy = deepCopyScanner.getCopy();
        String afterCopyCode = "class PlaceHold {" + copy.toString() + "}";

        OriginalCode = clearAllSpaces(OriginalCode);
        afterCopyCode = clearAllSpaces(afterCopyCode);

        assertEquals("Code not equal in " + path.toString(), OriginalCode, afterCopyCode);
    }

    private String clearAllSpaces(String code) {
        code = code.replaceAll("\\s", "");
        return code;
    }

}