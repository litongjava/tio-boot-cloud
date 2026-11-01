package nexus.io.tio.boot.cloud.gateway.route;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A route as written in the configuration.
 *
 * <pre>
 * gateway.routes[0].id=im
 * gateway.routes[0].uri=lb://im-server
 * gateway.routes[0].predicates[0]=Path=/im/**
 * gateway.routes[0].filters[0]=StripPrefix=1
 * gateway.routes[0].order=0
 * </pre>
 *
 * The uri is {@code lb://service} (load-balanced HTTP),
 * {@code lb:ws://service} (load-balanced WebSocket) or a fixed
 * {@code http://}, {@code https://} or {@code ws://} address.
 */
public class RouteDefinition {
  private String id;
  private String uri;
  private List<String> predicates = new ArrayList<>();
  private List<String> filters = new ArrayList<>();
  private int order;
  private Map<String, String> metadata = new LinkedHashMap<>();

  public RouteDefinition() {
  }

  public RouteDefinition(String id, String uri) {
    this.id = id;
    this.uri = uri;
  }

  public RouteDefinition predicate(String predicate) {
    predicates.add(predicate);
    return this;
  }

  public RouteDefinition filter(String filter) {
    filters.add(filter);
    return this;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public List<String> getPredicates() {
    return predicates;
  }

  public void setPredicates(List<String> predicates) {
    this.predicates = predicates;
  }

  public List<String> getFilters() {
    return filters;
  }

  public void setFilters(List<String> filters) {
    this.filters = filters;
  }

  public int getOrder() {
    return order;
  }

  public RouteDefinition setOrder(int order) {
    this.order = order;
    return this;
  }

  public Map<String, String> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }
}
