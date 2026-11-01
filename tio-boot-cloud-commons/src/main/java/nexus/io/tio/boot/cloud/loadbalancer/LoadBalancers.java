package nexus.io.tio.boot.cloud.loadbalancer;

import nexus.io.tio.utils.environment.EnvUtils;

/**
 * Builds the load balancer described by the configuration:
 * <ul>
 * <li>{@code cloud.loadbalancer.type}: {@code round-robin} (default) or
 * {@code random}</li>
 * <li>{@code cloud.loadbalancer.metadata-header}: request header for gray
 * releases, e.g. {@code gray_version}; when set, instances whose metadata
 * matches the header value are preferred</li>
 * <li>{@code cloud.loadbalancer.metadata-key}: instance metadata key compared
 * with the header, defaults to the header name</li>
 * </ul>
 */
public class LoadBalancers {

  public static LoadBalancer fromConfig() {
    String type = EnvUtils.get("cloud.loadbalancer.type", "round-robin");
    LoadBalancer base = "random".equalsIgnoreCase(type) ? new RandomLoadBalancer() : new RoundRobinLoadBalancer();
    String header = EnvUtils.get("cloud.loadbalancer.metadata-header");
    if (header == null || header.trim().isEmpty()) {
      return base;
    }
    String key = EnvUtils.get("cloud.loadbalancer.metadata-key", header.trim());
    return new MetadataLoadBalancer(header.trim(), key, base);
  }
}
