package nexus.io.tio.boot.cloud.client;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import nexus.io.tio.utils.json.JsonUtils;

/**
 * A call to another service, independent of the instance that will serve it.
 *
 * <pre>
 * ServiceRequest.get("/api/user/1").header("tenantId", "1");
 * ServiceRequest.post("/api/user").json(user);
 * </pre>
 */
public class ServiceRequest {
  private final String method;
  private final String path;
  private final Map<String, String> headers = new LinkedHashMap<>();
  private byte[] body;
  private String contentType;

  public ServiceRequest(String method, String path) {
    this.method = method.toUpperCase();
    this.path = path.startsWith("/") ? path : "/" + path;
  }

  public static ServiceRequest get(String path) {
    return new ServiceRequest("GET", path);
  }

  public static ServiceRequest post(String path) {
    return new ServiceRequest("POST", path);
  }

  public static ServiceRequest put(String path) {
    return new ServiceRequest("PUT", path);
  }

  public static ServiceRequest delete(String path) {
    return new ServiceRequest("DELETE", path);
  }

  public ServiceRequest header(String name, String value) {
    if (name != null && value != null) {
      headers.put(name, value);
    }
    return this;
  }

  /**
   * Adds the header only when it is not set yet.
   */
  public ServiceRequest headerIfAbsent(String name, String value) {
    if (name == null || value == null) {
      return this;
    }
    for (String existing : headers.keySet()) {
      if (existing.equalsIgnoreCase(name)) {
        return this;
      }
    }
    headers.put(name, value);
    return this;
  }

  /**
   * Sets a JSON body.
   *
   * @param value object to serialize, or a JSON string
   */
  public ServiceRequest json(Object value) {
    String json = value instanceof String ? (String) value : JsonUtils.toJson(value);
    this.body = json.getBytes(StandardCharsets.UTF_8);
    this.contentType = "application/json; charset=utf-8";
    return this;
  }

  public ServiceRequest body(byte[] body, String contentType) {
    this.body = body;
    this.contentType = contentType;
    return this;
  }

  public String getMethod() {
    return method;
  }

  public String getPath() {
    return path;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public byte[] getBody() {
    return body;
  }

  public String getContentType() {
    return contentType;
  }

  /**
   * @return true for methods that can be repeated safely
   */
  public boolean isIdempotent() {
    return "GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method);
  }
}
