package repair.pattern.serialize;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import repair.FileUtils;
import repair.pattern.Pattern;

import java.io.*;
import java.nio.file.Path;
import java.util.Optional;

public class Serializer {
    private static final Logger logger = LoggerFactory.getLogger(Serializer.class);

    public static void serializeToDisk(Pattern pattern, Path path) {
        FileUtils.ensureDirectoryExists(path);
        try (FileOutputStream fos = new FileOutputStream(path.toFile());
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(pattern);
        } catch (IOException e) {
            logger.error("Failed to serialize pattern to disk", e);
        }
    }

    public static Optional<Pattern> deserializeFromDisk(Path path) {
        try (FileInputStream fis = new FileInputStream(path.toFile());
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            Pattern pattern = (Pattern) ois.readObject();
            return Optional.of(pattern);
        } catch (IOException | ClassNotFoundException e) {
            logger.error("Failed to deserialize pattern from disk", e);
        }
        return Optional.empty();
    }

}
