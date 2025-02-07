package repair.main;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.common.CodeChangeInfo;
import repair.common.CodeChangeInfoReader;
import repair.pattern.Pattern;
import repair.pattern.serialize.JsonSerializer;
import repair.pattern.serialize.Serializer;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static repair.common.Utils.generatePattern;

public class Extract {
    private final static Logger logger = LoggerFactory.getLogger(Extract.class);

    private final static List<Pair<String, String>> possibleNamePairs = List.of(
            Pair.of("before.java", "after.java"),
            Pair.of("error.java", "correct.java"),
            Pair.of("left.java", "right.java")
    );

    public static void main(String[] args) {
        if (args.length < 4) {
            logger.error("Please given the arguments java -jar Main.jar extract [patternPair] [serializePath] [jsonSerializePath]");
            return;
        }

        Path patternPath = Path.of(args[1]);
        Path serializePath = Path.of(args[2]);
        Path jsonSerializePath = Path.of(args[3]);

        Pattern pattern;
        Path patternInfoPath = patternPath.resolve("info.json");
        if (patternInfoPath.toFile().exists()) {
            CodeChangeInfo patternInfo = CodeChangeInfoReader.readCCInfo(patternInfoPath);
            if (patternInfo == null) {
                logger.error("Failed to read pattern info from: {}", patternInfoPath);
                return;
            }

            pattern = generatePattern(patternPath, patternInfo.getSignatureBefore(), patternInfo.getSignatureAfter());
        } else {
            Optional<Pair<Path, Path>> possibleJavaPair = findPossibleJavaPair(patternPath);
            if (possibleJavaPair.isEmpty()) {
                logger.error("Failed to find possible java pair in: {}", patternPath);
                return;
            }

            pattern = generatePattern(possibleJavaPair.get().getLeft(), possibleJavaPair.get().getRight());
        }

        Serializer.serializeToDisk(pattern, serializePath);
        JsonSerializer.serializeToJson(pattern, jsonSerializePath);
    }

    private static Optional<Pair<Path, Path>> findPossibleJavaPair(Path patternCasePath) {
        for (Pair<String, String> pair : possibleNamePairs) {
            Path beforePath = patternCasePath.resolve(pair.getLeft());
            Path afterPath = patternCasePath.resolve(pair.getRight());

            if (beforePath.toFile().exists() && afterPath.toFile().exists()) {
                return Optional.of(Pair.of(beforePath, afterPath));
            }
        }

        return Optional.empty();
    }
}
