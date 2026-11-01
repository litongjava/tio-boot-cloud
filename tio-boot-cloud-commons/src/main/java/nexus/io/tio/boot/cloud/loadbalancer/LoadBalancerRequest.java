package nexus.io.tio.boot.cloud.loadbalancer;

import java.util.Collections;
import java.util.Map;

/**
 * Hints for choosing an instance, such as the request headers used for gray
 * releases.
 */
public class LoadBalancerRequest {
  public static final LoadBalancerRequest EMPTY = new LoadBalancerRequest(Collections.<String, String>emptyMap());

  private final Map<String, String> headers;

  public LoadBalancerRequest(Map<String, String> headers) {
    this.headers = headers == null ? Collections.<String, String>emptyMap() : headers;
  }

  /**
   * @param name lower-case header name
   * @return header value or null
   */
  public String getHeader(String name) {
    return headers.get(name);
  }

  public Map<String, String> getHeaders() {
    return headers;
  }
}
