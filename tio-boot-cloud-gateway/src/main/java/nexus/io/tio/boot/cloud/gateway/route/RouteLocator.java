package nexus.io.tio.boot.cloud.gateway.route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.tio.boot.cloud.gateway.GatewayExchange;
import nexus.io.tio.utils.environment.EnvUtils;

/**
 * Holds the routes and finds the first one that matches a request. Routes come
 * from the configuration ({@code gateway.routes[i].*}) and from code; calling
 * {@link #refresh()} re-reads the configuration, for example after a change in
 * the configuration center.
 */
public class RouteLocator {
  private static final Logger log = LoggerFactory.getLogger(RouteLocator.class);

  public static final String PREFIX = "gateway.routes";

  private final List<RouteDefinition> codeRoutes = new CopyOnWriteArrayList<>();
  private volatile List<Route> routes = Collections.emptyList();

  /**
   * Adds a route defined in code. It is kept across refreshes.
   */
  public RouteLocator add(RouteDefinition definition) {
    codeRoutes.add(definition);
    refresh();
    return this;
  }

  /**
   * Rebuilds the routes from the configuration and the code routes. When a
   * configured route is invalid, the current routes are kept.
   */
  public synchronized void refresh() {
    List<RouteDefinition> definitions = new ArrayList<>(readConfig());
    definitions.addAll(codeRoutes);
    List<Route> next = new ArrayList<>();
    try {
      for (RouteDefinition definition : definitions) {
        next.add(new Route(definition));
      }
    } catch (RuntimeException e) {
      log.error("invalid gateway route, keeping the current routes: {}", e.getMessage());
      return;
    }
    next.sort(Comparator.comparingInt(Route::getOrder));
    routes = Collections.unmodifiableList(next);
    for (Route route : next) {
      log.info("gateway route {} -> {} predicates={} filters={}", route.getId(), route.getDefinition().getUri(),
          route.getDefinition().getPredicates(), route.getDefinition().getFilters());
    }
  }

  public Route match(GatewayExchange exchange) {
    for (Route route : routes) {
      if (route.matches(exchange)) {
        return route;
      }
    }
    return null;
  }

  public List<Route> getRoutes() {
    return routes;
  }

  /**
   * Reads {@code gateway.routes[0..n]} until an index has neither id nor uri.
   */
  public static List<RouteDefinition> readConfig() {
    List<RouteDefinition> result = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      String base = PREFIX + "[" + i + "]";
      String id = EnvUtils.get(base + ".id");
      String uri = EnvUtils.get(base + ".uri");
      if (id == null && uri == null) {
        break;
      }
      RouteDefinition definition = new RouteDefinition(id == null ? "route-" + i : id, uri);
      definition.setPredicates(readList(base + ".predicates"));
      definition.setFilters(readList(base + ".filters"));
      definition.setOrder(EnvUtils.getInt(base + ".order", 0));
      result.add(definition);
    }
    return result;
  }

  private static List<String> readList(String key) {
    List<String> values = new ArrayList<>();
    for (int i = 0; i < 1000; i++) {
      String value = EnvUtils.get(key + "[" + i + "]");
      if (value == null) {
        break;
      }
      values.add(value);
    }
    return values;
  }
}
