package repair;

import org.mozilla.universalchardet.UniversalDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Hashtable;
import java.util.Map;

public class FileUtils {
    private final static Logger logger = LoggerFactory.getLogger(FileUtils.class);

    public static Map<String, Integer> loadGenPatMap(Path mapFile) throws IOException {
        File file = mapFile.toFile();
        if (!file.exists()) {
            throw new IOException("Token mapping file does not exist : " + file.getAbsolutePath());
        }
        Map<String, Integer> map = new Hashtable<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file),
                StandardCharsets.UTF_8));
        String token = br.readLine();
        String number = br.readLine();
        int num;
        while (token != null && number != null) {
            try {
                num = Integer.parseInt(number);
                map.put(token, num);
            } catch (Exception ignored) {
            }
            token = br.readLine();
            number = br.readLine();
        }
        br.close();
        return map;
    }

    public static void ensureDirectoryExists(Path filePath) {
        Path dirPath = filePath.getParent();
        if (!Files.exists(dirPath)) {
            try {
                Files.createDirectories(dirPath);
            } catch (IOException e) {
                logger.error("Failed to create directory: " + dirPath, e);
            }
        }
    }

    @Deprecated
    public static String detectCharset(File file) {
        byte[] buf = new byte[4096];
        try (FileInputStream fis = new FileInputStream(file)) {
            UniversalDetector detector = new UniversalDetector(null);

            int nread;
            while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, nread);
            }
            detector.dataEnd();

            String encoding = detector.getDetectedCharset();
            detector.reset();
            return encoding;
        } catch (IOException e) {
            logger.error("Failed to detect charset for file: " + file, e);
            return null;
        }
    }
}
