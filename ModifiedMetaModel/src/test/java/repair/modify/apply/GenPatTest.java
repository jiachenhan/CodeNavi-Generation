package repair.modify.apply;

import com.sun.jdi.InternalException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;
import repair.ast.MoNode;
import repair.ast.parser.NodeParser;
import repair.modify.apply.match.MatchInstance;
import repair.modify.apply.match.Matcher;
import repair.modify.diff.DiffComparator;
import repair.pattern.Pattern;
import repair.pattern.abstraction.Abstractor;
import repair.pattern.abstraction.TermFrequencyAbstractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.Assert.fail;
import static repair.common.JDTUtils.genASTFromFile;
import static repair.common.JDTUtils.getOnlyMethodDeclaration;

public class GenPatTest {

//    private final Path datasetPath = Paths.get("E:/dataset/api/apache-API-cluster");
    private final Path datasetPath = Paths.get("E:/dataset/c3/drjava1");

    static class Result {
        int totalCount;
        int adaptedCount;
        int successCount;
        List<Path> failedPaths;
        public Result(int totalCount, int adaptedCount, int successCount, List<Path> failedPaths) {
            this.totalCount = totalCount;
            this.adaptedCount = adaptedCount;
            this.successCount = successCount;
            this.failedPaths = failedPaths;
        }
    }

    static class AdaptSuccessFlag {
        boolean adapted;
        boolean success;
        public AdaptSuccessFlag(boolean adapted, boolean success) {
            this.adapted = adapted;
            this.success = success;
        }
    }

    @Test
    public void GenPatClusterTest() {
        AtomicInteger total = new AtomicInteger();
        AtomicInteger adapted = new AtomicInteger();
        AtomicInteger success = new AtomicInteger();
        try (Stream<Path> projectStream = Files.list(datasetPath)) {
            projectStream.forEach(projectPath -> {
                Result result = processProject(projectPath);
                System.out.println("Project: " + projectPath.getFileName());
                System.out.println("Total: " + result.totalCount);
                System.out.println("Success: " + result.successCount);
                System.out.println("Failed: " + result.failedPaths.size());
                System.out.println("Failed paths: ");
                result.failedPaths.forEach(System.out::println);
                total.addAndGet(result.totalCount);
                adapted.addAndGet(result.adaptedCount);
                success.addAndGet(result.successCount);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Total: " + total);
        System.out.println("Adapted: " + adapted);
        System.out.println("adapted rate: " + (double) adapted.get() / total.get());
        System.out.println("Success: " + success);
        System.out.println("success rate: " + (double) success.get() / adapted.get());
    }

    @Test
    public void debug() {
        Path groupPath = datasetPath.resolve("14");
        Path patternCasePath = null;
        List<Path> otherCasesPath = null;

        // 处理第三级：case
        try (Stream<Path> caseStream = Files.list(groupPath)) {
            List<Path> caseList = caseStream.toList();
            if (caseList.size() < 2) {
                System.out.println("Case less than 2, skip");
            }
            patternCasePath = caseList.get(0);
            otherCasesPath = new ArrayList<>(caseList.subList(1, caseList.size()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert patternCasePath != null;
        Pattern pattern = generatePattern(patternCasePath);

        Abstractor abstractor = new TermFrequencyAbstractor();
        abstractor.doAbstraction(pattern);

        Path finalPatternCasePath = patternCasePath;
        AtomicBoolean adapted = new AtomicBoolean(false);
        AtomicBoolean success = new AtomicBoolean(false);
        otherCasesPath.forEach(otherCasePath -> {
            if(success.get()) {
                return;
            }
            Path codeBeforePath = otherCasePath.resolve("before.java");
            Path codeAfterPath = otherCasePath.resolve("after.java");
            String oracle = getOracle(codeAfterPath);

            CompilationUnit beforeCompilationUnit = genASTFromFile(codeBeforePath);
            Optional<MethodDeclaration> methodBefore = getOnlyMethodDeclaration(beforeCompilationUnit);
            if(methodBefore.isEmpty()) {
                fail("MethodDeclaration is not present");
            }

            NodeParser beforeParser = new NodeParser(codeBeforePath.toString(), beforeCompilationUnit);
            MoNode moMethodBefore = beforeParser.process(methodBefore.get());

            List<MatchInstance> matchInstances = Matcher.match(pattern, moMethodBefore).stream().limit(5).toList();
            if(matchInstances.isEmpty()) {
                System.out.println("No match found in " + otherCasePath);
            }

            for (MatchInstance matchInstance : matchInstances) {
                if(!matchInstance.isLegal()) {
                    continue;
                }
                adapted.set(true);
                ApplyModification applyModification = new ApplyModification(pattern, moMethodBefore, matchInstance);
                try {
                    applyModification.apply();
                } catch (ModificationException e) {
                    e.printStackTrace();
                }

                String afterCopyCode = "class PlaceHold {" + applyModification.getRight().toString() + "}";
                afterCopyCode = clearAllSpaces(afterCopyCode);

                System.out.println("Oracle: " + oracle);
                System.out.println("After: " + afterCopyCode);

                if(oracle.equals(afterCopyCode)) {
                    success.set(true);
                    break;
                } else {
                    success.set(false);
                }
            }
        });
    }

    private Result processProject(Path projectPath) {
        System.out.println("Processing project: " + projectPath.getFileName());
        AtomicInteger totalCount = new AtomicInteger();
        AtomicInteger adaptedCount = new AtomicInteger();
        AtomicInteger successCount = new AtomicInteger();
        List<Path> allFailedPaths = new ArrayList<>();

        // 处理第二级：group
        try (Stream<Path> groupStream = Files.list(projectPath)) {
            groupStream.forEach(groupPath -> {
                totalCount.getAndIncrement();
                List<Path> failedPaths = new ArrayList<>();
                AdaptSuccessFlag adaptSuccessFlag = processGroup(groupPath, failedPaths);
                if(adaptSuccessFlag.adapted) {
                    adaptedCount.getAndIncrement();
                    if(adaptSuccessFlag.success) {
                        successCount.getAndIncrement();
                    } else {
                        allFailedPaths.addAll(failedPaths);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Result(totalCount.get(), adaptedCount.get(), successCount.get(), allFailedPaths);
    }

    private String getOracle(Path patternAfterPath) {
        String oracle = null;
        try {
            oracle = Files.readString(patternAfterPath);
            oracle = clearAllSpaces(oracle);
        } catch (IOException e) {
            System.out.println("Error reading origin file");
            fail();
        }
        return oracle;
    }

    private AdaptSuccessFlag processGroup(Path groupPath, List<Path> failedPaths) {
        System.out.println("Processing group: " + groupPath.getFileName());

        Path patternCasePath = null;
        List<Path> otherCasesPath = null;

        // 处理第三级：case
        try (Stream<Path> caseStream = Files.list(groupPath)) {
            List<Path> caseList = caseStream.toList();
            if (caseList.size() < 2) {
                System.out.println("Case less than 2, skip");
            }
            patternCasePath = caseList.get(0);
            otherCasesPath = new ArrayList<>(caseList.subList(1, caseList.size()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert patternCasePath != null;
        Pattern pattern = generatePattern(patternCasePath);

        Abstractor abstractor = new TermFrequencyAbstractor();
        abstractor.doAbstraction(pattern);

        Path finalPatternCasePath = patternCasePath;
        AtomicBoolean adapted = new AtomicBoolean(false);
        AtomicBoolean success = new AtomicBoolean(false);
        otherCasesPath.forEach(otherCasePath -> {
            if(success.get()) {
                return;
            }
            Path codeBeforePath = otherCasePath.resolve("before.java");
            Path codeAfterPath = otherCasePath.resolve("after.java");
            String oracle = getOracle(codeAfterPath);

            CompilationUnit beforeCompilationUnit = genASTFromFile(codeBeforePath);
            Optional<MethodDeclaration> methodBefore = getOnlyMethodDeclaration(beforeCompilationUnit);
            if(methodBefore.isEmpty()) {
                fail("MethodDeclaration is not present");
            }

            NodeParser beforeParser = new NodeParser(codeBeforePath.toString(), beforeCompilationUnit);
            MoNode moMethodBefore = beforeParser.process(methodBefore.get());

            List<MatchInstance> matchInstances = Matcher.match(pattern, moMethodBefore).stream().limit(5).toList();
            if(matchInstances.isEmpty()) {
                System.out.println("No match found in " + otherCasePath);
            }

            for (MatchInstance matchInstance : matchInstances) {
                if(!matchInstance.isLegal()) {
                    continue;
                }
                adapted.set(true);
                try {

                    ApplyModification applyModification = new ApplyModification(pattern, moMethodBefore, matchInstance);
                    applyModification.apply();

                    String afterCopyCode = "class PlaceHold {" + applyModification.getRight().toString() + "}";
                    afterCopyCode = clearAllSpaces(afterCopyCode);


                    if (oracle.equals(afterCopyCode)) {
                        success.set(true);
                        break;
                    } else {
                        success.set(false);
                        failedPaths.add(finalPatternCasePath);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    failedPaths.add(finalPatternCasePath);
                }
            }
        });

        return new AdaptSuccessFlag(adapted.get(), success.get());
    }

    private Pattern generatePattern(Path patternCase) {
        System.out.println("Processing case: " + patternCase.getFileName());
        Path patternBeforePath = patternCase.resolve("before.java");
        Path patternAfterPath = patternCase.resolve("after.java");

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

        return new Pattern(moMethodBefore, moMethodAfter, DiffComparator.Mode.MOVE_MODE,
                beforeParser.getIdentifierManager(), afterParser.getIdentifierManager());
    }



    private String clearAllSpaces(String code) {
        code = code.replaceAll("\\s", "");
        return code;
    }
}
