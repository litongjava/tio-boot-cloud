package nexus.io.tio.boot.cloud.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.tio.utils.environment.EnvUtils;
import nexus.io.tio.utils.environment.Prop;
import nexus.io.tio.utils.environment.PropUtils;

/**
 * Merges configuration from a configuration center into the values read by
 * {@code EnvUtils}.
 * <p>
 * Remote values rank above {@code app.properties} and its profile files and
 * below command-line arguments, system properties and environment variables,
 * so a single instance can still be overridden at startup. Sources are merged
 * in the order they were first applied; a later source wins for the same key.
 * When a source changes, removed keys fall back to the local file value.
 * <p>
 * Apply remote values after {@code EnvUtils.load(args)} so command-line
 * arguments and local files are already in place.
 */
public class RemoteConfig {
  private static final Logger log = LoggerFactory.getLogger(RemoteConfig.class);

  private static final Map<String, Map<String, String>> sources = new LinkedHashMap<>();
  private static final Map<String, String> localValues = new HashMap<>();
  private static Map<String, String> merged = new HashMap<>();
  private static final List<ConfigChangeListener> listeners = new CopyOnWriteArrayList<>();

  /**
   * Replaces the values of one source and applies the merged result.
   *
   * @param source name of the source, such as a data id
   * @param values flat key/value pairs
   * @return keys whose effective remote value changed
   */
  public static Set<String> apply(String source, Map<String, String> values) {
    Set<String> changed;
    synchronized (RemoteConfig.class) {
      sources.put(source, new LinkedHashMap<>(values));
      Map<String, String> next = new HashMap<>();
      for (Map<String, String> sourceValues : sources.values()) {
        next.putAll(sourceValues);
      }
      changed = new LinkedHashSet<>();
      Properties properties = PropUtils.getProp().getProperties();
      for (Map.Entry<String, String> e : next.entrySet()) {
        String key = e.getKey();
        if (!Objects.equals(merged.get(key), e.getValue())) {
          changed.add(key);
        }
        if (!merged.containsKey(key) && !localValues.containsKey(key)) {
          localValues.put(key, properties.getProperty(key));
        }
      }
      for (String key : merged.keySet()) {
        if (!next.containsKey(key)) {
          changed.add(key);
        }
      }

      Prop prop = new Prop();
      prop.getProperties().putAll(next);
      PropUtils.append(prop);
      for (String key : merged.keySet()) {
        if (!next.containsKey(key)) {
          String local = localValues.remove(key);
          if (local == null) {
            properties.remove(key);
          } else {
            properties.setProperty(key, local);
          }
        }
      }
      merged = next;
    }
    if (!changed.isEmpty()) {
      log.info("remote config {} applied, {} keys changed", source, changed.size());
      for (ConfigChangeListener listener : listeners) {
        try {
          listener.onChange(source, Collections.unmodifiableSet(changed));
        } catch (Exception e) {
          log.error("config change listener failed", e);
        }
      }
    }
    return changed;
  }

  /**
   * Parses and applies a source.
   *
   * @param source  data id or file name, its extension selects the format
   * @param content configuration text
   * @return keys whose effective remote value changed
   */
  public static Set<String> apply(String source, String content) {
    return apply(source, ConfigFormats.parse(source, content));
  }

  public static void addListener(ConfigChangeListener listener) {
    listeners.add(listener);
  }

  public static void removeListener(ConfigChangeListener listener) {
    listeners.remove(listener);
  }

  /**
   * @return a copy of the merged remote values
   */
  public static synchronized Map<String, String> values() {
    return new HashMap<>(merged);
  }

  /**
   * @param prefix key prefix, e.g. {@code nacos.discovery.metadata.}
   * @return local and remote values whose key starts with the prefix
   */
  public static Map<String, String> valuesWithPrefix(String prefix) {
    Map<String, String> result = new LinkedHashMap<>();
    Properties properties = PropUtils.getProp().getProperties();
    for (String key : properties.stringPropertyNames()) {
      if (key.startsWith(prefix)) {
        result.put(key.substring(prefix.length()), EnvUtils.get(key));
      }
    }
    return result;
  }

  /** Forgets all sources, for tests. */
  public static synchronized void reset() {
    Properties properties = PropUtils.getProp().getProperties();
    for (String key : merged.keySet()) {
      String local = localValues.get(key);
      if (local == null) {
        properties.remove(key);
      } else {
        properties.setProperty(key, local);
      }
    }
    sources.clear();
    localValues.clear();
    merged = new HashMap<>();
    listeners.clear();
  }
}
