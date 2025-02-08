package repair.pattern;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.FileUtils;
import repair.apply.diff.DiffComparator;
import repair.ast.MoNode;
import repair.ast.parser.NodeParser;
import repair.dsl.kirin.QueryGenerator;
import repair.dsl.kirin.query.Query;
import repair.pattern.serialize.JsonSerializer;
import repair.pattern.serialize.Serializer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.Assert.fail;
import static repair.common.JDTUtils.genASTFromFile;
import static repair.common.JDTUtils.getOnlyMethodDeclaration;

public class NotLogicTest {
    private static final Logger logger = LoggerFactory.getLogger(NotLogicTest.class);

    private final Path datasetPath = Path.of("D:/datas/sample_100_dataset");
    private final Path queryPath = Path.of("D:/workspace/CodeNavi-Generation/07dsl");
    private final Path patternBasePath = Path.of("D:/workspace/CodeNavi-Generation/01pattern");
    private final Path patternInfoBasePath = Path.of("D:/workspace/CodeNavi-Generation/02pattern-info");

    private void processDirectory(Consumer<Path> action) {
        try (Stream<Path> stream = Files.walk(datasetPath)) {
            List<Path> paths = stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals("error.java"))
                    .sorted(Comparator.comparing(path -> {
                        Path parent = path.getParent();
                        if (parent != null) return parent.getFileName().toString();
                        return "";
                    })).toList();
            for (Path path : paths) {
                action.accept(path);
            }
        } catch (IOException e) {
            logger.error("Failed to walk dataset directory: {}", datasetPath, e);
        }
    }

    @Test
    public void storeQuery() {
        Consumer<Path> action = path -> {
            try {
                logger.info("Processing: {}", path);
                Path dslPath = queryPath.resolve(path.getParent().getFileName().toString()).resolve("dsl.kirin");
                FileUtils.ensureDirectoryExists(dslPath);
                Pattern pattern = generatePattern(path);
                Files.writeString(dslPath, generateQuery(pattern));
            } catch (IOException e) {
                logger.error("Failed to store query", e);
            }
        };
        processDirectory(action);
    }

    @Test
    public void serializePattern() {
        Consumer<Path> action = path -> {
            logger.info("Processing: {}", path);
            Path patternPath = patternBasePath.resolve("ori").resolve("test").resolve(path.getParent().getFileName() + ".ser");
            Path patternInfoPath = patternInfoBasePath.resolve("input").resolve("test").resolve(path.getParent().getFileName() + ".json");
            Pattern pattern = generatePattern(path);
            Serializer.serializeToDisk(pattern, patternPath);
            JsonSerializer.serializeToJson(pattern, patternInfoPath);
        };
        processDirectory(action);
    }

    @Test
    public void deserializeTest() {
        Path patternPath = Path.of("D:/workspace/CodeNavi-Generation/01pattern/ori/test/1acd.ser");
        Optional<Pattern> patternOpt = Serializer.deserializeFromDisk(patternPath);
        if (patternOpt.isEmpty()) {
            fail("Failed to read pattern from: " + patternPath);
        }
        Pattern pattern = patternOpt.get();
        System.out.println(pattern);
    }

    private Pattern generatePattern(Path errorPath) {
        Path correctPath = errorPath.resolveSibling("correct.java");

        CompilationUnit beforeCompilationUnit = genASTFromFile(errorPath);
        CompilationUnit afterCompilationUnit = genASTFromFile(correctPath);

        Optional<MethodDeclaration> methodBefore = getOnlyMethodDeclaration(beforeCompilationUnit);
        Optional<MethodDeclaration> methodAfter = getOnlyMethodDeclaration(afterCompilationUnit);

        if(methodBefore.isEmpty() || methodAfter.isEmpty()) {
            fail("MethodDeclaration is not present");
        }

        NodeParser beforeParser = new NodeParser(errorPath, beforeCompilationUnit);
        NodeParser afterParser = new NodeParser(correctPath, afterCompilationUnit);

        MoNode moMethodBefore = beforeParser.process(methodBefore.get());
        MoNode moMethodAfter = afterParser.process(methodAfter.get());

        return new Pattern(moMethodBefore, moMethodAfter, DiffComparator.Mode.MOVE_MODE);
    }


    private String generateQuery(Pattern pattern) {
        Query query = QueryGenerator.getInstance().generate(pattern);
        return query.prettyPrint();
    }

}
