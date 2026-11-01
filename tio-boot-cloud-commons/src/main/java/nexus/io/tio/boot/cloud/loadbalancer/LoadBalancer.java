package nexus.io.tio.boot.cloud.loadbalancer;

import java.util.List;

import nexus.io.tio.boot.cloud.discovery.ServiceInstance;

/**
 * Chooses one instance of a service for a call.
 */
public interface LoadBalancer {

  /**
   * @param serviceId service name
   * @param instances candidate instances, not empty
   * @param request   hints such as request headers
   * @return the chosen instance
   */
  ServiceInstance choose(String serviceId, List<ServiceInstance> instances, LoadBalancerRequest request);
}
