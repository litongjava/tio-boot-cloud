package nexus.io.tio.boot.cloud.loadbalancer;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import nexus.io.tio.boot.cloud.discovery.ServiceInstance;

/**
 * Takes the instances of each service in turn.
 */
public class RoundRobinLoadBalancer implements LoadBalancer {
  private final ConcurrentHashMap<String, AtomicInteger> positions = new ConcurrentHashMap<>();

  @Override
  public ServiceInstance choose(String serviceId, List<ServiceInstance> instances, LoadBalancerRequest request) {
    AtomicInteger position = positions.computeIfAbsent(serviceId, k -> new AtomicInteger());
    int index = (position.getAndIncrement() & Integer.MAX_VALUE) % instances.size();
    return instances.get(index);
  }
}
