package repair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Hashtable;
import java.util.Map;

public class FileUtils {
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
}
