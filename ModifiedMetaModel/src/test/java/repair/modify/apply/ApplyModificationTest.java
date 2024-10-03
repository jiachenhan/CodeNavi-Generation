package repair.modify.apply;

import com.sun.jdi.InternalException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;
import repair.modify.apply.ApplyModification;
import repair.modify.apply.match.MatchInstance;
import repair.modify.apply.match.MatchMock;
import repair.ast.MoNode;
import repair.ast.parser.NodeParser;
import repair.ast.visitor.DeepCopyScanner;
import repair.modify.diff.DiffComparator;
import repair.pattern.Pattern;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static repair.common.JDTUtils.genASTFromFile;
import static repair.common.JDTUtils.getOnlyMethodDeclaration;

public class ApplyModificationTest {
    private final Path datasetPath = Paths.get("E:/dataset/api/apache-API-cluster");

    private final List<Path> excludedPaths = List.of(
            // gumtree match error
            Path.of("E:\\dataset\\api\\apache-API-cluster\\archiva\\15\\1e1f7cdacd0118a5fb9a707871c7b7100b7f09d2--DefaultRepositoryGroupService-DefaultRepositoryGroupService--101-103_102-105\\before.java"),
            Path.of("E:\\dataset\\api\\apache-API-cluster\\archiva\\15\\1e1f7cdacd0118a5fb9a707871c7b7100b7f09d2--DefaultRepositoryGroupService-DefaultRepositoryGroupService--85-87_85-88\\before.java")
    );

    @Test
    public void clusterDatasetAllTest() {
        AtomicInteger count = new AtomicInteger();
        AtomicInteger success = new AtomicInteger();
        List<Path> failedPaths = new ArrayList<>();
        List<Path> wrongMatchPaths = new ArrayList<>();
        List<Path> mayIndexErrPaths = new ArrayList<>();
        try(Stream<Path> javaStream = Files.walk(datasetPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().equals("before.java"))) {
            javaStream.forEach(patternBeforePath -> {
                success.getAndIncrement();

                if(excludedPaths.contains(patternBeforePath)) {
                    return;
                }

                try{
                    System.out.println("Processing: " + patternBeforePath);
                    Path patternAfterPath = patternBeforePath.resolveSibling("after.java");

                    CompilationUnit beforeCompilationUnit = genASTFromFile(patternBeforePath);
                    CompilationUnit afterCompilationUnit = genASTFromFile(patternAfterPath);

                    Optional<MethodDeclaration> methodBefore = getOnlyMethodDeclaration(beforeCompilationUnit);
                    Optional<MethodDeclaration> methodAfter = getOnlyMethodDeclaration(afterCompilationUnit);

                    if(methodBefore.isEmpty() || methodAfter.isEmpty()) {
                        fail("MethodDeclaration is not present");
                    }

                    NodeParser beforeParser = new NodeParser(patternBeforePath, beforeCompilationUnit);
                    NodeParser afterParser = new NodeParser(patternAfterPath, afterCompilationUnit);

                    MoNode moMethodBefore = beforeParser.process(methodBefore.get());
                    MoNode moMethodAfter = afterParser.process(methodAfter.get());

                    Pattern pattern = new Pattern(moMethodBefore, moMethodAfter, DiffComparator.Mode.MOVE_MODE);
                    DeepCopyScanner deepCopyScanner = new DeepCopyScanner(moMethodBefore);
                    MoNode copyBefore = deepCopyScanner.getCopy();

                    // try repair
                    MatchInstance matchMock = MatchMock.match(pattern, copyBefore, deepCopyScanner.getCopyMap());

                    ApplyModification applyModification = new ApplyModification(pattern, copyBefore, matchMock);
                    applyModification.apply();

                    String afterCopyCode = "class PlaceHold {" + applyModification.getRight().toString() + "}";
                    afterCopyCode = clearAllSpaces(afterCopyCode);

                    // get oracle
                    String oracle = null;
                    try {
                        oracle = Files.readString(patternAfterPath);
                        oracle = clearAllSpaces(oracle);
                    } catch (IOException e) {
                        System.out.println("Error reading origin file");
                        fail();
                    }

                    if(oracle.equals(afterCopyCode)) {
                        count.getAndIncrement();
                    } else {
                        if(oracle.length() == afterCopyCode.length()) {
                            mayIndexErrPaths.add(patternBeforePath);
                        }
                        System.out.println("Error in " + patternBeforePath.toString());
                        failedPaths.add(patternBeforePath);
                    }
                } catch (InternalException e) {
                    e.printStackTrace();
                    wrongMatchPaths.add(patternBeforePath);
                } catch (Throwable e) {
                    e.printStackTrace();
                    failedPaths.add(patternBeforePath);
                }
//                assertEquals("Code not equal in " + patternBeforePath.toString(), oracle, afterCopyCode);
            });

            if(!failedPaths.isEmpty()) {
                System.out.println("Failed paths: ");
                failedPaths.forEach(System.out::println);
            }
            System.out.println("Cycle Risks: " + wrongMatchPaths.size());
            if(!wrongMatchPaths.isEmpty()) {
                System.out.println("Cycle Risk paths: ");
                wrongMatchPaths.forEach(System.out::println);
            }
            System.out.println("May Index Error: " + mayIndexErrPaths.size());
            if(!mayIndexErrPaths.isEmpty()) {
                System.out.println("May Index Error paths: ");
                mayIndexErrPaths.forEach(System.out::println);
            }

            System.out.println("Success: " + success.get() + ", Correct: " + count.get());
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void debug() {
        Path base = Paths.get("E:/dataset/api/apache-API-cluster");
        String projectName = "zookeeper";
        String groupName = "2";
        String caseName = "190a227aa9d4655ebfe6ba9f5c2da426da8c5d98--DataTree-DataTree--623-624_598-599";
//        String groupName = "24";
//        String caseName = "03036e9b3dcaada18a8e39c8f03dc4dbb0090777--SchedulerThriftInterface-SchedulerThriftInterface--492-493_495-496";
        Path patternBeforePath = base.resolve(projectName).resolve(groupName).resolve(caseName).resolve("before.java");
        Path patternAfterPath = patternBeforePath.resolveSibling("after.java");

        CompilationUnit beforeCompilationUnit = genASTFromFile(patternBeforePath);
        CompilationUnit afterCompilationUnit = genASTFromFile(patternAfterPath);

        Optional<MethodDeclaration> methodBefore = getOnlyMethodDeclaration(beforeCompilationUnit);
        Optional<MethodDeclaration> methodAfter = getOnlyMethodDeclaration(afterCompilationUnit);

        if(methodBefore.isEmpty() || methodAfter.isEmpty()) {
            fail("MethodDeclaration is not present");
        }

        NodeParser beforeParser = new NodeParser(patternBeforePath, beforeCompilationUnit);
        NodeParser afterParser = new NodeParser(patternAfterPath, afterCompilationUnit);

        MoNode moMethodBefore = beforeParser.process(methodBefore.get());
        MoNode moMethodAfter = afterParser.process(methodAfter.get());

        Pattern pattern = new Pattern(moMethodBefore, moMethodAfter, DiffComparator.Mode.MOVE_MODE);
        DeepCopyScanner deepCopyScanner = new DeepCopyScanner(moMethodBefore);
        MoNode copyBefore = deepCopyScanner.getCopy();

        // try repair
        MatchInstance matchMock = MatchMock.match(pattern, copyBefore, deepCopyScanner.getCopyMap());

        ApplyModification applyModification = new ApplyModification(pattern, copyBefore, matchMock);
        applyModification.apply();
        System.out.println(applyModification.getRight());

        String afterCopyCode = "class PlaceHold {" + applyModification.getRight().toString() + "}";
        afterCopyCode = clearAllSpaces(afterCopyCode);

        // get oracle
        String oracle = null;
        try {
            oracle = Files.readString(patternAfterPath);
            oracle = clearAllSpaces(oracle);
        } catch (IOException e) {
            System.out.println("Error reading origin file");
            fail();
        }


        assertEquals("Code not equal in " + patternBeforePath.toString(), oracle, afterCopyCode);
    }


    private String clearAllSpaces(String code) {
        code = code.replaceAll("\\s", "");
        return code;
    }

}