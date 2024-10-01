package repair.pattern.serialize;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.pattern.Pattern;

public class JsonSerializer {
    private static final Logger logger = LoggerFactory.getLogger(JsonSerializer.class);

    public static String serializeToJSON(Pattern pattern) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(pattern);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize pattern to JSON", e);
        }
        return null;
    }
}
