package repair.pattern.serialize;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.FileUtils;
import repair.ast.MoNode;
import repair.pattern.Pattern;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class JsonSerializer {
    private static final Logger logger = LoggerFactory.getLogger(JsonSerializer.class);

    public static String serializeToJson(Pattern pattern) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(pattern);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize pattern to JSON", e);
        }
        return null;
    }
    public static void serializeToJson(List<MoNode> nodes, Path path) {
        FileUtils.ensureDirectoryExists(path.getParent());
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(path.toFile(), nodes);
        } catch (IOException e) {
            logger.error("Failed to serialize nodes list to JSON", e);
        }
    }

    public static void serializeToJson(Pattern pattern, Path path) {
        FileUtils.ensureDirectoryExists(path);
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(path.toFile(), pattern);
        } catch (IOException e) {
            logger.error("Failed to serialize pattern to JSON", e);
        }
    }
}
