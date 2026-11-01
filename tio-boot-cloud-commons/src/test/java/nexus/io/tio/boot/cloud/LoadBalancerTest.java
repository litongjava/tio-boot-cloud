package nexus.io.tio.boot.cloud;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import nexus.io.tio.boot.cloud.discovery.ServiceInstance;
import nexus.io.tio.boot.cloud.loadbalancer.LoadBalancerRequest;
import nexus.io.tio.boot.cloud.loadbalancer.MetadataLoadBalancer;
import nexus.io.tio.boot.cloud.loadbalancer.RoundRobinLoadBalancer;

public class LoadBalancerTest {
  private final ServiceInstance a = new ServiceInstance("svc", "10.0.0.1", 80);
  private final ServiceInstance b = new ServiceInstance("svc", "10.0.0.2", 80);
  private final List<ServiceInstance> both = Arrays.asList(a, b);

  @Test
  public void roundRobinAlternates() {
    RoundRobinLoadBalancer lb = new RoundRobinLoadBalancer();
    assertEquals(a, lb.choose("svc", both, LoadBalancerRequest.EMPTY));
    assertEquals(b, lb.choose("svc", both, LoadBalancerRequest.EMPTY));
    assertEquals(a, lb.choose("svc", both, LoadBalancerRequest.EMPTY));
  }

  @Test
  public void metadataSelectsGrayInstances() {
    b.getMetadata().put("gray_version", "v2");
    MetadataLoadBalancer lb = new MetadataLoadBalancer("gray_version", "gray_version", new RoundRobinLoadBalancer());
    LoadBalancerRequest gray = new LoadBalancerRequest(Collections.singletonMap("gray_version", "v2"));
    for (int i = 0; i < 4; i++) {
      assertEquals(b, lb.choose("svc", both, gray));
    }
    LoadBalancerRequest unknown = new LoadBalancerRequest(Collections.singletonMap("gray_version", "v9"));
    assertEquals(a, lb.choose("svc", both, unknown));
    assertEquals(b, lb.choose("svc", both, unknown));
  }
}
