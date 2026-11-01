package nexus.io.tio.boot.cloud.discovery;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import nexus.io.tio.utils.environment.EnvUtils;

/**
 * Discovery from fixed addresses, for local development and tests without a
 * registry. Addresses come from {@link #add(String, String...)} or from the
 * configuration key {@code cloud.discovery.simple.<serviceId>}, for example
 * {@code cloud.discovery.simple.user-service=http://127.0.0.1:8081,http://127.0.0.1:8082}.
 */
public class SimpleDiscoveryClient implements DiscoveryClient {
  public static final String PREFIX = "cloud.discovery.simple.";

  private final Map<String, List<ServiceInstance>> services = new ConcurrentHashMap<>();

  public SimpleDiscoveryClient add(String serviceId, String... uris) {
    List<ServiceInstance> list = new ArrayList<>();
    for (String uri : uris) {
      list.add(parse(serviceId, uri));
    }
    services.put(serviceId, Collections.unmodifiableList(list));
    return this;
  }

  @Override
  public List<ServiceInstance> getInstances(String serviceId) {
    List<ServiceInstance> list = services.get(serviceId);
    if (list != null) {
      return list;
    }
    String configured = EnvUtils.get(PREFIX + serviceId);
    if (configured == null || configured.trim().isEmpty()) {
      return Collections.emptyList();
    }
    List<ServiceInstance> parsed = new ArrayList<>();
    for (String uri : configured.split(",")) {
      if (!uri.trim().isEmpty()) {
        parsed.add(parse(serviceId, uri.trim()));
      }
    }
    return parsed;
  }

  @Override
  public List<String> getServices() {
    return new ArrayList<>(services.keySet());
  }

  private static ServiceInstance parse(String serviceId, String value) {
    URI uri = URI.create(value.contains("://") ? value : "http://" + value);
    boolean secure = "https".equalsIgnoreCase(uri.getScheme()) || "wss".equalsIgnoreCase(uri.getScheme());
    int port = uri.getPort() > 0 ? uri.getPort() : (secure ? 443 : 80);
    return new ServiceInstance(serviceId, uri.getHost(), port).setSecure(secure);
  }
}
