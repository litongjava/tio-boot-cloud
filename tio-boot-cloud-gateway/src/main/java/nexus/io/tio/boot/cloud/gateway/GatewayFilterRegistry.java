package nexus.io.tio.boot.cloud.gateway;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import nexus.io.tio.boot.cloud.gateway.filter.GatewayFilter;
import nexus.io.tio.boot.cloud.gateway.filter.GlobalFilter;
import nexus.io.tio.boot.cloud.gateway.route.Route;

/**
 * Global filters of the gateway, kept sorted by order.
 */
public class GatewayFilterRegistry {
  private final List<GlobalFilter> globalFilters = new CopyOnWriteArrayList<>();
  private volatile List<GlobalFilter> sorted = new ArrayList<>();

  public synchronized void add(GlobalFilter filter) {
    globalFilters.add(filter);
    List<GlobalFilter> next = new ArrayList<>(globalFilters);
    next.sort(Comparator.comparingInt(GlobalFilter::getOrder));
    sorted = next;
  }

  /**
   * @return global filters followed by the route's filters
   */
  public List<GatewayFilter> filtersFor(Route route) {
    List<GatewayFilter> filters = new ArrayList<>(sorted);
    filters.addAll(route.getFilters());
    return filters;
  }
}
