package repair.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class CodeChangeInfoReader {
    private static final Logger logger = LoggerFactory.getLogger(CodeChangeInfoReader.class);

    public static CodeChangeInfo readCCInfo(Path jsonPath) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // 读取JSON文件并将其映射到Person对象
            return objectMapper.readValue(jsonPath.toFile(), CodeChangeInfo.class);
        } catch (IOException e) {
            logger.error("Failed to read file: " + jsonPath, e);
            return null;
        }
    }
}
