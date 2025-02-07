package repair.common;

import org.eclipse.jdt.core.dom.*;
import org.junit.Test;
import repair.ast.MoNode;
import repair.ast.parser.NodeParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static repair.common.JDTUtils.*;

public class MethodSignatureTest {
    private final Path apiDatasetPath = Paths.get("E:/dataset/api/apache-API-cluster");
    private final Path c3DatasetPath = Paths.get("E:/dataset/c3/junit");

    @Test
    public void extractSignatureTest() {
        try(Stream<Path> javaStream = Files.walk(apiDatasetPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".java"))) {
            javaStream.forEach(path -> {
                System.out.println("Processing: " + path);

                CompilationUnit compilationUnit = genASTFromFile(path);
                Optional<MethodDeclaration> onlyMethodDeclaration = getOnlyMethodDeclaration(compilationUnit);
                if(onlyMethodDeclaration.isEmpty()) {
                    fail("MethodDeclaration is not present");
                }


                NodeParser parser = new NodeParser(path, compilationUnit);
                MoNode moNode = parser.process(onlyMethodDeclaration.get());

                MethodSignature methodSignature = MethodSignature.parseFunctionSignature(moNode.toString());
                System.out.println("Signature: " + methodSignature.toString());
                assertTrue("Signature not equal in " + path.toString(), methodSignature.sameSignature(onlyMethodDeclaration.get()));
            });
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void singleTest() {
        String projectName = "incubator-doris";
        String GroupName = "4";
        Path groupPath = apiDatasetPath.resolve(projectName).resolve(GroupName);

        String casePattern = "69c90b1640d566ab03a045f89a0e50b8b6f7b00f--LoadAction-LoadAction--87-88_87-88";
        Path path = groupPath.resolve(casePattern).resolve("after.java");

        CompilationUnit compilationUnit = genASTFromFile(path);
        Optional<MethodDeclaration> onlyMethodDeclaration = getOnlyMethodDeclaration(compilationUnit);
        if(onlyMethodDeclaration.isEmpty()) {
            fail("MethodDeclaration is not present");
        }
        NodeParser parser = new NodeParser(path, compilationUnit);
        MoNode moNode = parser.process(onlyMethodDeclaration.get());

        MethodSignature methodSignature = MethodSignature.parseFunctionSignature(moNode.toString());
        System.out.println("Signature: " + methodSignature.toString());
        assertTrue("Signature not equal in " + path.toString(), methodSignature.sameSignature(onlyMethodDeclaration.get()));
    }

    @Test
    public void debugTest() {
        String groupName = "639";
        String caseName = "1";
        Path path = c3DatasetPath.resolve(groupName).resolve(caseName).resolve("info.json");

        Path beforePath = path.resolveSibling("before.java");
        Path afterPath = path.resolveSibling("after.java");

        CodeChangeInfo codeChangeInfo = CodeChangeInfoReader.readCCInfo(path);
        assert codeChangeInfo != null;
        MethodSignature methodSignatureBefore = MethodSignature.parseFunctionSignature(codeChangeInfo.getSignatureBefore());
        MethodSignature methodSignatureAfter = MethodSignature.parseFunctionSignature(codeChangeInfo.getSignatureAfter());

        // check before
        CompilationUnit beforeCompilationUnit = genASTFromFile(beforePath);
        Optional<MethodDeclaration> methodBefore = getMethodDeclaration(beforeCompilationUnit, methodSignatureBefore);
        if(methodBefore.isEmpty()) {
            fail("MethodDeclaration is not present");
        }

        NodeParser beforeParser = new NodeParser(beforePath, beforeCompilationUnit);
        MoNode moMethodBefore = beforeParser.process(methodBefore.get());
        System.out.println("beforeCode: " + moMethodBefore.toString());
        MethodSignature Signature = MethodSignature.parseFunctionSignature(moMethodBefore.toString());
        assertTrue("Signature not equal in " + path.toString(), Signature.sameSignature(methodBefore.get()));

        // check after
        CompilationUnit afterCompilationUnit = genASTFromFile(afterPath);
        Optional<MethodDeclaration> methodAfter = getMethodDeclaration(afterCompilationUnit, methodSignatureAfter);
        if(methodAfter.isEmpty()) {
            fail("MethodDeclaration is not present");
        }

        NodeParser afterParser = new NodeParser(afterPath, afterCompilationUnit);
        MoNode moMethodAfter = afterParser.process(methodAfter.get());
        System.out.println("afterCode: " + moMethodAfter.toString());
        Signature = MethodSignature.parseFunctionSignature(moMethodAfter.toString());
        assertTrue("Signature not equal in " + path.toString(), Signature.sameSignature(methodAfter.get()));
    }

    @Test
    public void codeChangeInfoTest() {
        List<Path> errorPaths = new ArrayList<>();
        try(Stream<Path> javaStream = Files.walk(c3DatasetPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".json"))) {
            javaStream.forEach(path -> {
                System.out.println("Processing: " + path);

                Path beforePath = path.resolveSibling("before.java");
                Path afterPath = path.resolveSibling("after.java");

                CodeChangeInfo codeChangeInfo = CodeChangeInfoReader.readCCInfo(path);
                assert codeChangeInfo != null;
                MethodSignature methodSignatureBefore = MethodSignature.parseFunctionSignature(codeChangeInfo.getSignatureBefore());
                MethodSignature methodSignatureAfter = MethodSignature.parseFunctionSignature(codeChangeInfo.getSignatureAfter());

                // check before
                CompilationUnit beforeCompilationUnit = genASTFromFile(beforePath);
                Optional<MethodDeclaration> methodBefore = getMethodDeclaration(beforeCompilationUnit, methodSignatureBefore);
                if(methodBefore.isEmpty()) {
                    System.out.println("MethodDeclaration is not present");
                    errorPaths.add(beforePath);
                    return;
                }

                NodeParser beforeParser = new NodeParser(beforePath, beforeCompilationUnit);
                MoNode moMethodBefore = beforeParser.process(methodBefore.get());
                MethodSignature Signature = MethodSignature.parseFunctionSignature(moMethodBefore.toString());
                assertTrue("Signature not equal in " + path.toString(), Signature.sameSignature(methodBefore.get()));

                // check after
                CompilationUnit afterCompilationUnit = genASTFromFile(afterPath);
                Optional<MethodDeclaration> methodAfter = getMethodDeclaration(afterCompilationUnit, methodSignatureAfter);
                if(methodAfter.isEmpty()) {
                    System.out.println("MethodDeclaration is not present");
                    errorPaths.add(afterPath);
                    return;
                }

                NodeParser afterParser = new NodeParser(afterPath, afterCompilationUnit);
                MoNode moMethodAfter = afterParser.process(methodAfter.get());
                Signature = MethodSignature.parseFunctionSignature(moMethodAfter.toString());
                assertTrue("Signature not equal in " + path.toString(), Signature.sameSignature(methodAfter.get()));
            });

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
        System.out.println(errorPaths.size());
        errorPaths.forEach(System.out::println);
    }

}