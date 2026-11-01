package nexus.io.tio.boot.cloud.loadbalancer;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import nexus.io.tio.boot.cloud.discovery.ServiceInstance;

/**
 * Picks an instance at random, in proportion to its weight.
 */
public class RandomLoadBalancer implements LoadBalancer {

  @Override
  public ServiceInstance choose(String serviceId, List<ServiceInstance> instances, LoadBalancerRequest request) {
    double total = 0;
    for (ServiceInstance instance : instances) {
      total += Math.max(instance.getWeight(), 0);
    }
    if (total <= 0) {
      return instances.get(ThreadLocalRandom.current().nextInt(instances.size()));
    }
    double point = ThreadLocalRandom.current().nextDouble(total);
    for (ServiceInstance instance : instances) {
      point -= Math.max(instance.getWeight(), 0);
      if (point < 0) {
        return instance;
      }
    }
    return instances.get(instances.size() - 1);
  }
}
