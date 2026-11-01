package nexus.io.tio.boot.cloud;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nexus.io.tio.boot.cloud.discovery.DiscoveryClient;
import nexus.io.tio.boot.cloud.discovery.ServiceInstance;
import nexus.io.tio.boot.cloud.discovery.ServiceRegistry;
import nexus.io.tio.boot.cloud.discovery.SimpleDiscoveryClient;
import nexus.io.tio.boot.cloud.loadbalancer.LoadBalancer;
import nexus.io.tio.boot.cloud.loadbalancer.LoadBalancerRequest;
import nexus.io.tio.boot.cloud.loadbalancer.RoundRobinLoadBalancer;

/**
 * Holds the discovery client, registry and load balancer used by the service
 * client and the gateway. A registry module such as Nacos sets them at
 * startup; without one, {@link SimpleDiscoveryClient} reads fixed addresses
 * from the configuration.
 */
public class CloudContext {
  private static volatile DiscoveryClient discoveryClient = new SimpleDiscoveryClient();
  private static volatile ServiceRegistry serviceRegistry;
  private static volatile LoadBalancer loadBalancer = new RoundRobinLoadBalancer();
  private static volatile ServiceInstance localInstance;

  public static List<ServiceInstance> getInstances(String serviceId) {
    List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
    return instances == null ? new ArrayList<ServiceInstance>() : instances;
  }

  /**
   * @param serviceId service name
   * @param request   hints for the load balancer
   * @return an instance, or null when the service has none
   */
  public static ServiceInstance choose(String serviceId, LoadBalancerRequest request) {
    return choose(serviceId, request, null);
  }

  /**
   * @param serviceId service name
   * @param request   hints for the load balancer
   * @param exclude   instances already tried, may be null
   * @return an instance, or null when no instance is left
   */
  public static ServiceInstance choose(String serviceId, LoadBalancerRequest request,
      Collection<ServiceInstance> exclude) {
    List<ServiceInstance> instances = getInstances(serviceId);
    if (exclude != null && !exclude.isEmpty() && !instances.isEmpty()) {
      List<ServiceInstance> left = new ArrayList<>(instances.size());
      for (ServiceInstance instance : instances) {
        if (!contains(exclude, instance)) {
          left.add(instance);
        }
      }
      instances = left;
    }
    if (instances.isEmpty()) {
      return null;
    }
    return loadBalancer.choose(serviceId, instances, request == null ? LoadBalancerRequest.EMPTY : request);
  }

  private static boolean contains(Collection<ServiceInstance> list, ServiceInstance instance) {
    for (ServiceInstance item : list) {
      if (item.getHost().equals(instance.getHost()) && item.getPort() == instance.getPort()) {
        return true;
      }
    }
    return false;
  }

  public static DiscoveryClient getDiscoveryClient() {
    return discoveryClient;
  }

  public static void setDiscoveryClient(DiscoveryClient discoveryClient) {
    CloudContext.discoveryClient = discoveryClient;
  }

  public static ServiceRegistry getServiceRegistry() {
    return serviceRegistry;
  }

  public static void setServiceRegistry(ServiceRegistry serviceRegistry) {
    CloudContext.serviceRegistry = serviceRegistry;
  }

  public static LoadBalancer getLoadBalancer() {
    return loadBalancer;
  }

  public static void setLoadBalancer(LoadBalancer loadBalancer) {
    CloudContext.loadBalancer = loadBalancer;
  }

  /**
   * @return the instance this application registered, or null
   */
  public static ServiceInstance getLocalInstance() {
    return localInstance;
  }

  public static void setLocalInstance(ServiceInstance localInstance) {
    CloudContext.localInstance = localInstance;
  }
}
