package nexus.io.tio.boot.cloud.client;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import nexus.io.tio.boot.cloud.discovery.ServiceInstance;
import nexus.io.tio.utils.json.JsonUtils;

/**
 * Response of a service call.
 */
public class ServiceResponse {
  private final int status;
  private final Map<String, String> headers;
  private final byte[] body;
  private final ServiceInstance instance;

  public ServiceResponse(int status, Map<String, String> headers, byte[] body, ServiceInstance instance) {
    this.status = status;
    this.headers = headers;
    this.body = body;
    this.instance = instance;
  }

  public boolean isSuccessful() {
    return status >= 200 && status < 300;
  }

  public int getStatus() {
    return status;
  }

  /**
   * @param name header name, case-insensitive
   */
  public String getHeader(String name) {
    return headers.get(name.toLowerCase());
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public byte[] getBody() {
    return body;
  }

  public String getBodyString() {
    return body == null ? null : new String(body, StandardCharsets.UTF_8);
  }

  public <T> T json(Class<T> type) {
    String text = getBodyString();
    return text == null ? null : JsonUtils.parse(text, type);
  }

  /**
   * @return the instance that served the call
   */
  public ServiceInstance getInstance() {
    return instance;
  }
}
