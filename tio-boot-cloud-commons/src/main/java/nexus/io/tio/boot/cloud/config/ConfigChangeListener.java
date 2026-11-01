package nexus.io.tio.boot.cloud.config;

import java.util.Set;

/**
 * Notified after remote configuration changed and was applied.
 */
@FunctionalInterface
public interface ConfigChangeListener {

  /**
   * @param source      name of the changed source, such as a data id
   * @param changedKeys keys that were added, changed or removed
   */
  void onChange(String source, Set<String> changedKeys);
}
