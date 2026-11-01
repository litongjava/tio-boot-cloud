package nexus.io.tio.boot.cloud.nacos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.context.Context;
import nexus.io.context.ServerListener;
import nexus.io.tio.boot.cloud.CloudContext;
import nexus.io.tio.boot.cloud.discovery.ServiceInstance;
import nexus.io.tio.boot.cloud.discovery.ServiceRegistry;
import nexus.io.tio.utils.environment.EnvUtils;

/**
 * Registers the application once the server listens and deregisters it before
 * the server stops.
 */
public class NacosRegistrationListener implements ServerListener {
  private static final Logger log = LoggerFactory.getLogger(NacosRegistrationListener.class);

  private final ServiceRegistry registry;
  private volatile ServiceInstance registered;

  public NacosRegistrationListener(ServiceRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void beforeStart(Class<?>[] primarySources, String[] args) {
  }

  @Override
  public void afterStarted(Class<?>[] primarySources, String[] args, Context context) {
    String serviceName = NacosProperties.serviceName();
    if (serviceName == null || serviceName.trim().isEmpty()) {
      throw new IllegalStateException("Set app.name or nacos.discovery.service to register with Nacos");
    }
    int port = EnvUtils.getInt(NacosProperties.DISCOVERY_PORT, context.getPort());
    ServiceInstance instance = new ServiceInstance(serviceName, NacosProperties.registerIp(), port);
    instance.setWeight(Double.parseDouble(EnvUtils.get(NacosProperties.DISCOVERY_WEIGHT, "1")));
    instance.getMetadata().putAll(NacosProperties.metadata());
    registry.register(instance);
    registered = instance;
    CloudContext.setLocalInstance(instance);
    log.info("registered {} to nacos {} as {}:{}", serviceName, NacosProperties.serverAddr(), instance.getHost(), port);
  }

  @Override
  public void beforeStop() {
    ServiceInstance instance = registered;
    if (instance == null) {
      return;
    }
    registered = null;
    try {
      registry.deregister(instance);
      log.info("deregistered {} from nacos", instance);
    } catch (Exception e) {
      log.error("deregister {} failed: {}", instance, e.getMessage());
    }
  }

  @Override
  public void afterStopped() {
    TioBootNacos.shutdown();
  }
}
