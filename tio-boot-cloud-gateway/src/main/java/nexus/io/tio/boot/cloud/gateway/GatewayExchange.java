package nexus.io.tio.boot.cloud.gateway;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import nexus.io.tio.boot.cloud.gateway.route.Route;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;

/**
 * State of one request passing through the gateway. Filters read and change
 * the request headers, the forwarded path and the response headers here.
 */
public class GatewayExchange {
  private final HttpRequest request;
  private final HttpResponse response;
  private final boolean webSocket;
  private final Map<String, Object> attributes = new HashMap<>();
  private Route route;
  private String path;
  private String query;

  public GatewayExchange(HttpRequest request, HttpResponse response, boolean webSocket) {
    this.request = request;
    this.response = response;
    this.webSocket = webSocket;
    this.path = request.getRequestLine().getPath();
    String queryString = request.getRequestLine().getQueryString();
    this.query = queryString == null || queryString.isEmpty() ? null : queryString;
  }

  public HttpRequest getRequest() {
    return request;
  }

  public HttpResponse getResponse() {
    return response;
  }

  /**
   * @return true while resolving the upstream of a WebSocket handshake
   */
  public boolean isWebSocket() {
    return webSocket;
  }

  public String getMethod() {
    return request.getRequestLine().getMethod().name();
  }

  /**
   * @return the path that will be forwarded, after StripPrefix and similar
   *         filters
   */
  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path == null || path.isEmpty() ? "/" : path;
  }

  /**
   * @return the raw query string without '?', or null
   */
  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getPathAndQuery() {
    return query == null ? path : path + "?" + query;
  }

  public String getRequestHeader(String name) {
    return request.getHeader(name.toLowerCase(Locale.ROOT));
  }

  /**
   * Sets a header on the request sent to the upstream.
   */
  public void setRequestHeader(String name, String value) {
    request.addHeader(name.toLowerCase(Locale.ROOT), value);
  }

  public void removeRequestHeader(String name) {
    request.getHeaders().remove(name.toLowerCase(Locale.ROOT));
  }

  /**
   * Sets a header on the response to the client. Headers returned by the
   * upstream with the same name take precedence.
   */
  public void setResponseHeader(String name, String value) {
    response.addHeader(name.toLowerCase(Locale.ROOT), value);
  }

  public Route getRoute() {
    return route;
  }

  public void setRoute(Route route) {
    this.route = route;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  @SuppressWarnings("unchecked")
  public <T> T getAttribute(String name) {
    return (T) attributes.get(name);
  }

  public void setAttribute(String name, Object value) {
    attributes.put(name, value);
  }
}
