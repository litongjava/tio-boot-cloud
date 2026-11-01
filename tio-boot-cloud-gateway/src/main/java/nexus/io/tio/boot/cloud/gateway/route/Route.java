package nexus.io.tio.boot.cloud.gateway.route;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import nexus.io.tio.boot.cloud.gateway.GatewayExchange;
import nexus.io.tio.boot.cloud.gateway.filter.GatewayFilter;
import nexus.io.tio.boot.cloud.gateway.filter.GatewayFilters;
import nexus.io.tio.boot.cloud.gateway.predicate.RoutePredicate;
import nexus.io.tio.boot.cloud.gateway.predicate.RoutePredicates;

/**
 * A compiled route: predicates, filters and the target.
 */
public class Route {
  private final RouteDefinition definition;
  private final List<RoutePredicate> predicates;
  private final List<GatewayFilter> filters;
  private final boolean loadBalanced;
  private final boolean webSocket;
  private final String serviceId;
  private final String baseUri;

  public Route(RouteDefinition definition) {
    this.definition = definition;
    if (definition.getUri() == null || definition.getUri().trim().isEmpty()) {
      throw new IllegalArgumentException("Route " + definition.getId() + " has no uri");
    }
    String uri = definition.getUri().trim();
    String lower = uri.toLowerCase(Locale.ROOT);
    if (lower.startsWith("lb:ws://") || lower.startsWith("lb:wss://")) {
      loadBalanced = true;
      webSocket = true;
      serviceId = uri.substring(uri.indexOf("://") + 3).replaceAll("/.*$", "");
      baseUri = null;
    } else if (lower.startsWith("lb://")) {
      loadBalanced = true;
      webSocket = false;
      serviceId = uri.substring(5).replaceAll("/.*$", "");
      baseUri = null;
    } else {
      URI parsed = URI.create(uri);
      loadBalanced = false;
      webSocket = "ws".equalsIgnoreCase(parsed.getScheme()) || "wss".equalsIgnoreCase(parsed.getScheme());
      serviceId = null;
      String path = parsed.getRawPath() == null ? "" : parsed.getRawPath();
      baseUri = parsed.getScheme() + "://" + parsed.getRawAuthority() + (path.equals("/") ? "" : path);
    }

    List<RoutePredicate> predicateList = new ArrayList<>();
    for (String predicate : definition.getPredicates()) {
      predicateList.add(RoutePredicates.create(predicate));
    }
    this.predicates = Collections.unmodifiableList(predicateList);
    List<GatewayFilter> filterList = new ArrayList<>();
    GatewayFilters.addAll(definition.getFilters(), filterList);
    this.filters = Collections.unmodifiableList(filterList);
  }

  public boolean matches(GatewayExchange exchange) {
    for (RoutePredicate predicate : predicates) {
      if (!predicate.test(exchange)) {
        return false;
      }
    }
    return true;
  }

  public String getId() {
    return definition.getId();
  }

  public int getOrder() {
    return definition.getOrder();
  }

  public RouteDefinition getDefinition() {
    return definition;
  }

  public List<GatewayFilter> getFilters() {
    return filters;
  }

  public boolean isLoadBalanced() {
    return loadBalanced;
  }

  /**
   * @return true when the uri is {@code lb:ws://} or {@code ws://}
   */
  public boolean isWebSocket() {
    return webSocket;
  }

  public String getServiceId() {
    return serviceId;
  }

  /**
   * @return the fixed target, e.g. {@code http://127.0.0.1:8080}, when the
   *         route is not load balanced
   */
  public String getBaseUri() {
    return baseUri;
  }
}
