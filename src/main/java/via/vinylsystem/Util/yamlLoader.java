package via.vinylsystem.Util;

import org.yaml.snakeyaml.Yaml;
import java.io.FileInputStream;
import java.util.Map;

public class yamlLoader {

    public static Map<String, Object> loadConfig(String filePath) throws Exception {
        Yaml yaml = new Yaml();
        try (FileInputStream in = new FileInputStream(filePath)) {
            return yaml.load(in);
        }
    }
}
