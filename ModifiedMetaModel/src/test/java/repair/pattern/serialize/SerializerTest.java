package repair.pattern.serialize;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;
import repair.FileUtils;
import repair.ast.MoNode;
import repair.ast.parser.NodeParser;
import repair.apply.diff.DiffComparator;
import repair.common.CodeChangeInfo;
import repair.common.CodeChangeInfoReader;
import repair.common.MethodSignature;
import repair.pattern.Pattern;
import repair.pattern.abstraction.Abstractor;
import repair.pattern.abstraction.TermFrequencyAbstractor;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static repair.common.JDTUtils.*;
import static repair.common.JDTUtils.getDeclaration;
import static repair.main.Main.generatePattern;

public class SerializerTest {
    private final Path datasetPath = Paths.get("E:/dataset/c3/drjava1");
    private final Path serializePath = Paths.get("01pattern");
    private final Path jsonSerializePath = Paths.get("02pattern-info");

    @Test
    public void serializeTest() {
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
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert patternCasePath != null;
        Pattern pattern = generatePattern(patternCasePath);

        Serializer.serializeToDisk(pattern, serializePath.resolve("ori").resolve("pattern.ser"));

        Abstractor abstractor = new TermFrequencyAbstractor();
        abstractor.doAbstraction(pattern);

        Serializer.serializeToDisk(pattern, serializePath.resolve("abs").resolve("pattern_abstracted.ser"));
    }

    @Test
    public void jsonSerializeTest() {
        String group = "41225";
        Path groupPath = Path.of("E:/dataset/c3/c3_random_1000/jdt").resolve(group);
        Path patternCasePath = groupPath.resolve("1");

        Path patternInfoPath = patternCasePath.resolve("info.json");
        CodeChangeInfo patternInfo = CodeChangeInfoReader.readCCInfo(patternInfoPath);
        if (patternInfo == null) {
            System.out.println("Failed to read pattern info from: " + patternInfoPath);
            return;
        }

        Pattern pattern = generatePattern(patternCasePath, patternInfo.getSignatureBefore(), patternInfo.getSignatureAfter());

        System.out.println("Serializing pattern: " + patternCasePath);
        String json = JsonSerializer.serializeToJson(pattern);
        Path patternJsonPath = jsonSerializePath.resolve("c3_random_1000").resolve("jdt").resolve(group)
                .resolve(patternCasePath.getFileName() + ".json");
        FileUtils.ensureDirectoryExists(patternJsonPath);
        try(FileWriter file = new FileWriter(patternJsonPath.toFile())) {
            file.write(Objects.requireNonNull(json));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void deserializeTest() {
        Pattern pattern = Serializer.deserializeFromDisk(serializePath.resolve("pattern.ser")).orElse(null);
        assertNotNull(pattern);

        Pattern patternAbstracted = Serializer.deserializeFromDisk(serializePath.resolve("pattern_abstracted.ser")).orElse(null);
        assertNotNull(patternAbstracted);
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

        NodeParser beforeParser = new NodeParser(patternBeforePath, beforeCompilationUnit);
        NodeParser afterParser = new NodeParser(patternAfterPath, afterCompilationUnit);

        MoNode moMethodBefore = beforeParser.process(methodBefore.get());
        MoNode moMethodAfter = afterParser.process(methodAfter.get());

        return new Pattern(moMethodBefore, moMethodAfter, DiffComparator.Mode.MOVE_MODE,
                beforeParser.getIdentifierManager(), afterParser.getIdentifierManager());
    }

    public static Pattern generatePattern(Path patternCase, String beforeSignature, String afterSignature) {
        Path patternBeforePath = patternCase.resolve("before.java");
        Path patternAfterPath = patternCase.resolve("after.java");

        CompilationUnit beforeCompilationUnit = genASTFromFile(patternBeforePath);
        CompilationUnit afterCompilationUnit = genASTFromFile(patternAfterPath);

        MethodSignature methodSignatureBefore = MethodSignature.parseFunctionSignature(beforeSignature);
        MethodSignature methodSignatureAfter = MethodSignature.parseFunctionSignature(afterSignature);

        Optional<MethodDeclaration> methodBefore = getDeclaration(beforeCompilationUnit, methodSignatureBefore);
        Optional<MethodDeclaration> methodAfter = getDeclaration(afterCompilationUnit, methodSignatureAfter);

        if(methodBefore.isEmpty() || methodAfter.isEmpty()) {
            fail("MethodDeclaration is not present");
        }

        NodeParser beforeParser = new NodeParser(patternBeforePath, beforeCompilationUnit);
        NodeParser afterParser = new NodeParser(patternAfterPath, afterCompilationUnit);

        MoNode moMethodBefore = beforeParser.process(methodBefore.get());
        MoNode moMethodAfter = afterParser.process(methodAfter.get());

        return new Pattern(moMethodBefore, moMethodAfter, DiffComparator.Mode.MOVE_MODE);
    }

}