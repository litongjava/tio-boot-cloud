package nexus.io.tio.boot.cloud.discovery;

/**
 * Registers the current application in a service registry.
 */
public interface ServiceRegistry {

  void register(ServiceInstance instance);

  void deregister(ServiceInstance instance);
}
