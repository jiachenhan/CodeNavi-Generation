package repair.main;

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
import java.util.Objects;

import static repair.main.Main.generatePattern;

public class Extract {
    private final static Logger logger = LoggerFactory.getLogger(Extract.class);

    public static void main(String[] args) {
        if (args.length < 4) {
            logger.error("Please given the arguments java -jar Main.jar extract [patternPair] [serializePath] [jsonSerializePath]");
            return;
        }

        Path patternPath = Path.of(args[1]);
        Path serializePath = Path.of(args[2]);
        Path jsonSerializePath = Path.of(args[3]);

        Path patternInfoPath = patternPath.resolve("info.json");
        CodeChangeInfo patternInfo = CodeChangeInfoReader.readCCInfo(patternInfoPath);
        if (patternInfo == null) {
            logger.error("Failed to read pattern info from: " + patternInfoPath);
            return;
        }

        // 1. 生成/抽象pattern
        Pattern pattern = generatePattern(patternPath, patternInfo.getSignatureBefore(), patternInfo.getSignatureAfter());

        Serializer.serializeToDisk(pattern, serializePath);
        JsonSerializer.serializeToJson(pattern, jsonSerializePath);
    }
}
