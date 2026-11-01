package nexus.io.tio.boot.cloud.nacos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;

import nexus.io.tio.boot.cloud.CloudContext;
import nexus.io.tio.boot.cloud.loadbalancer.LoadBalancers;
import nexus.io.tio.boot.server.TioBootServer;
import nexus.io.tio.utils.environment.EnvUtils;

/**
 * Connects a tio-boot application to Nacos.
 *
 * <pre>
 * public static void main(String[] args) {
 *   TioApplication.run(App.class, () -&gt; {
 *     TioBootNacos.start();
 *     // routes ...
 *   }, args);
 * }
 * </pre>
 *
 * The configuration ({@code nacos.config.data-ids}) is loaded automatically
 * while tio-boot starts, before server settings are read, so remote values can
 * also set server.port. {@link #start()} makes {@code CloudContext} discover
 * services through Nacos and registers the application once the server is
 * listening.
 */
public class TioBootNacos {
  private static final Logger log = LoggerFactory.getLogger(TioBootNacos.class);

  private static volatile NamingService namingService;
  private static volatile ConfigService configService;
  private static volatile boolean configLoaded;

  /**
   * Loads {@code nacos.config.data-ids} once. Enabled when data ids are set,
   * unless {@code nacos.config.enabled=false}.
   */
  public static synchronized void loadConfig() {
    if (configLoaded) {
      return;
    }
    configLoaded = true;
    boolean enabled = EnvUtils.getBoolean(NacosProperties.CONFIG_ENABLED, !NacosProperties.dataIds().isEmpty());
    if (!enabled) {
      return;
    }
    new NacosConfigLoader(configService()).load();
  }

  /**
   * Loads configuration, sets up discovery and schedules registration. Call it
   * while the application is configured, e.g. in a BootConfiguration.
   */
  public static synchronized void start() {
    loadConfig();
    CloudContext.setLoadBalancer(LoadBalancers.fromConfig());
    if (!EnvUtils.getBoolean(NacosProperties.DISCOVERY_ENABLED, true)) {
      return;
    }
    NamingService naming = namingService();
    String group = NacosProperties.discoveryGroup();
    NacosServiceRegistry registry = new NacosServiceRegistry(naming, group);
    CloudContext.setDiscoveryClient(new NacosDiscoveryClient(naming, group));
    CloudContext.setServiceRegistry(registry);
    if (EnvUtils.getBoolean(NacosProperties.DISCOVERY_REGISTER_ENABLED, true)) {
      TioBootServer.me().addServerListener(new NacosRegistrationListener(registry));
    }
  }

  public static synchronized NamingService namingService() {
    if (namingService == null) {
      try {
        namingService = NacosFactory.createNamingService(NacosProperties.clientProperties());
      } catch (NacosException e) {
        throw new IllegalStateException("create nacos naming service failed: " + e.getErrMsg(), e);
      }
    }
    return namingService;
  }

  public static synchronized ConfigService configService() {
    if (configService == null) {
      try {
        configService = NacosFactory.createConfigService(NacosProperties.clientProperties());
      } catch (NacosException e) {
        throw new IllegalStateException("create nacos config service failed: " + e.getErrMsg(), e);
      }
    }
    return configService;
  }

  /**
   * Closes the Nacos clients. Called after the server stopped.
   */
  public static synchronized void shutdown() {
    try {
      if (namingService != null) {
        namingService.shutDown();
      }
      if (configService != null) {
        configService.shutDown();
      }
    } catch (NacosException e) {
      log.error("shutdown nacos client failed: {}", e.getErrMsg());
    } finally {
      namingService = null;
      configService = null;
      configLoaded = false;
    }
  }
}
