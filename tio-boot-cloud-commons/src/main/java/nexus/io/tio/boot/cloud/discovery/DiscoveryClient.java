package nexus.io.tio.boot.cloud.discovery;

import java.util.List;

/**
 * Looks up the running instances of a service.
 */
public interface DiscoveryClient {

  /**
   * @param serviceId service name
   * @return healthy instances, never null
   */
  List<ServiceInstance> getInstances(String serviceId);

  /**
   * @return names of the known services
   */
  List<String> getServices();
}
