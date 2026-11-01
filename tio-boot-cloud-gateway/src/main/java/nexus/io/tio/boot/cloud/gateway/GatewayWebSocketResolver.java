package nexus.io.tio.boot.cloud.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.tio.boot.cloud.CloudContext;
import nexus.io.tio.boot.cloud.discovery.ServiceInstance;
import nexus.io.tio.boot.cloud.gateway.filter.DefaultGatewayFilterChain;
import nexus.io.tio.boot.cloud.gateway.route.Route;
import nexus.io.tio.boot.cloud.gateway.route.RouteLocator;
import nexus.io.tio.boot.cloud.loadbalancer.LoadBalancerRequest;
import nexus.io.tio.boot.websocket.proxy.WebSocketProxyHandler;
import nexus.io.tio.boot.websocket.proxy.WebSocketUpstream;
import nexus.io.tio.boot.websocket.proxy.WebSocketUpstreamResolver;
import nexus.io.tio.core.ChannelContext;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;

/**
 * Chooses the upstream of a WebSocket handshake with the same routes and
 * filters as HTTP requests. A filter that answers directly (for example with
 * 401) refuses the connection.
 */
public class GatewayWebSocketResolver implements WebSocketUpstreamResolver {
  private static final Logger log = LoggerFactory.getLogger(GatewayWebSocketResolver.class);

  private static final String UPSTREAM = "gateway.websocket.upstream";

  private final RouteLocator routeLocator;
  private final GatewayFilterRegistry filterRegistry;

  public GatewayWebSocketResolver(RouteLocator routeLocator, GatewayFilterRegistry filterRegistry) {
    this.routeLocator = routeLocator;
    this.filterRegistry = filterRegistry;
  }

  @Override
  public WebSocketUpstream resolve(HttpRequest request, ChannelContext channelContext) throws Exception {
    GatewayExchange exchange = new GatewayExchange(request, new HttpResponse(request), true);
    Route route = routeLocator.match(exchange);
    if (route == null) {
      log.warn("no websocket route for {}", request.getRequestLine().getPath());
      return null;
    }
    exchange.setRoute(route);
    new DefaultGatewayFilterChain(filterRegistry.filtersFor(route), this::target).filter(exchange);
    String url = exchange.getAttribute(UPSTREAM);
    if (url == null) {
      return null;
    }
    return WebSocketProxyHandler.upstreamOf(url, request);
  }

  private HttpResponse target(GatewayExchange exchange) {
    Route route = exchange.getRoute();
    String base;
    if (route.isLoadBalanced()) {
      ServiceInstance instance = CloudContext.choose(route.getServiceId(),
          new LoadBalancerRequest(exchange.getRequest().getHeaders()));
      if (instance == null) {
        log.warn("no available instance of {}", route.getServiceId());
        return exchange.getResponse();
      }
      base = instance.getWebSocketUri();
    } else {
      base = route.getBaseUri().replaceFirst("^http", "ws");
    }
    exchange.setAttribute(UPSTREAM, base + exchange.getPathAndQuery());
    return exchange.getResponse();
  }
}
