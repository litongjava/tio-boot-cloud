package nexus.io.tio.boot.cloud.nacos;

import nexus.io.tio.boot.env.EnvLoadedListener;

/**
 * Loads {@code nacos.config.data-ids} while tio-boot starts, right after the
 * local configuration and before server settings such as server.port are
 * read. Registered through ServiceLoader, so adding this module to the
 * classpath is enough.
 */
public class NacosEnvLoadedListener implements EnvLoadedListener {

  @Override
  public void onEnvLoaded(String[] args) {
    TioBootNacos.loadConfig();
  }
}
