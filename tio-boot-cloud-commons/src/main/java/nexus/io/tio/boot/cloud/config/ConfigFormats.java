package nexus.io.tio.boot.cloud.config;

import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Parses configuration text into flat {@code key=value} pairs. YAML maps become
 * dotted keys and lists become indexed keys, so
 *
 * <pre>
 * gateway:
 *   routes:
 *     - id: im
 *       predicates:
 *         - Path=/im/**
 * </pre>
 *
 * becomes {@code gateway.routes[0].id=im} and
 * {@code gateway.routes[0].predicates[0]=Path=/im/**}.
 */
public class ConfigFormats {

  /**
   * @param name    data id or file name; {@code .yml} and {@code .yaml} are
   *                parsed as YAML, everything else as properties
   * @param content configuration text
   * @return flat key/value pairs in document order
   */
  public static Map<String, String> parse(String name, String content) {
    if (content == null || content.trim().isEmpty()) {
      return new LinkedHashMap<>();
    }
    String lower = name == null ? "" : name.toLowerCase(Locale.ROOT);
    if (lower.endsWith(".yml") || lower.endsWith(".yaml")) {
      return parseYaml(content);
    }
    return parseProperties(content);
  }

  public static Map<String, String> parseProperties(String content) {
    Properties properties = new Properties();
    try {
      properties.load(new StringReader(content));
    } catch (IOException e) {
      throw new IllegalArgumentException("Invalid properties", e);
    }
    Map<String, String> result = new LinkedHashMap<>();
    for (String key : properties.stringPropertyNames()) {
      result.put(key, properties.getProperty(key));
    }
    return result;
  }

  public static Map<String, String> parseYaml(String content) {
    Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
    Map<String, String> result = new LinkedHashMap<>();
    for (Object document : yaml.loadAll(content)) {
      flatten("", document, result);
    }
    return result;
  }

  private static void flatten(String prefix, Object value, Map<String, String> result) {
    if (value instanceof Map) {
      for (Map.Entry<?, ?> e : ((Map<?, ?>) value).entrySet()) {
        String key = String.valueOf(e.getKey());
        flatten(prefix.isEmpty() ? key : prefix + "." + key, e.getValue(), result);
      }
    } else if (value instanceof List) {
      List<?> list = (List<?>) value;
      for (int i = 0; i < list.size(); i++) {
        flatten(prefix + "[" + i + "]", list.get(i), result);
      }
    } else if (!prefix.isEmpty()) {
      result.put(prefix, value == null ? "" : String.valueOf(value));
    }
  }
}
