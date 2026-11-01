package nexus.io.tio.boot.cloud.loadbalancer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import nexus.io.tio.boot.cloud.discovery.ServiceInstance;

/**
 * Gray release by instance metadata. When the request carries the header (for
 * example {@code gray_version: v2}), only instances whose metadata has the same
 * value are used; otherwise, or when none match, all instances are used. The
 * final choice is made by the delegate.
 */
public class MetadataLoadBalancer implements LoadBalancer {
  private final String headerName;
  private final String metadataKey;
  private final LoadBalancer delegate;

  /**
   * @param headerName  request header carrying the wanted version
   * @param metadataKey instance metadata key holding the version
   * @param delegate    balancer used on the selected instances
   */
  public MetadataLoadBalancer(String headerName, String metadataKey, LoadBalancer delegate) {
    this.headerName = headerName.toLowerCase(Locale.ROOT);
    this.metadataKey = metadataKey;
    this.delegate = delegate;
  }

  @Override
  public ServiceInstance choose(String serviceId, List<ServiceInstance> instances, LoadBalancerRequest request) {
    String wanted = request == null ? null : request.getHeader(headerName);
    if (wanted != null && !wanted.isEmpty()) {
      List<ServiceInstance> matched = new ArrayList<>();
      for (ServiceInstance instance : instances) {
        if (wanted.equals(instance.getMetadata(metadataKey))) {
          matched.add(instance);
        }
      }
      if (!matched.isEmpty()) {
        return delegate.choose(serviceId, matched, request);
      }
    }
    return delegate.choose(serviceId, instances, request);
  }
}
