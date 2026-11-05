package nexus.io.tio.boot.cloud.gateway;

import nexus.io.tio.boot.cloud.CloudContext;
import nexus.io.tio.boot.cloud.config.RemoteConfig;
import nexus.io.tio.boot.cloud.gateway.filter.CorsGlobalFilter;
import nexus.io.tio.boot.cloud.gateway.filter.GlobalFilter;
import nexus.io.tio.boot.cloud.gateway.filter.TraceIdGlobalFilter;
import nexus.io.tio.boot.cloud.gateway.route.RouteDefinition;
import nexus.io.tio.boot.cloud.gateway.route.RouteLocator;
import nexus.io.tio.boot.cloud.loadbalancer.LoadBalancers;
import nexus.io.tio.boot.server.TioBootServer;
import nexus.io.tio.boot.websocket.proxy.WebSocketProxyHandler;
import nexus.io.tio.http.server.router.HttpRequestRouter;
import nexus.io.tio.utils.environment.EnvUtils;

/**
 * Turns a tio-boot application into an API gateway.
 *
 * <pre>
 * TioApplication.run(GatewayApp.class, () -&gt; {
 *   TioBootNacos.start();
 *   TioBootGateway.start();
 *   TioBootGateway.addGlobalFilter(new TokenFilter());
 * }, args);
 * </pre>
 *
 * Routes are read from {@code gateway.routes[i].*} and reloaded when the
 * configuration center changes a {@code gateway.} key. Every path that no
 * other handler of the application serves goes through the gateway, for HTTP
 * and for WebSocket. CORS ({@code gateway.cors.enabled}, default true) and
 * trace ids ({@code gateway.trace.enabled}, default true) are built in.
 */
public class TioBootGateway {
  private static final RouteLocator routeLocator = new RouteLocator();
  private static final GatewayFilterRegistry filterRegistry = new GatewayFilterRegistry();
  private static volatile boolean started;
  private static volatile GatewayHttpHandler httpHandler;

  public static synchronized void start() {
    if (started) {
      return;
    }
    started = true;
    if (CloudContext.getServiceRegistry() == null) {
      CloudContext.setLoadBalancer(LoadBalancers.fromConfig());
    }
    if (EnvUtils.getBoolean("gateway.cors.enabled", true)) {
      filterRegistry.add(new CorsGlobalFilter());
    }
    if (EnvUtils.getBoolean("gateway.trace.enabled", true)) {
      filterRegistry.add(new TraceIdGlobalFilter());
    }
    routeLocator.refresh();
    RemoteConfig.addListener((source, keys) -> {
      for (String key : keys) {
        if (key.startsWith(RouteLocator.PREFIX)) {
          routeLocator.refresh();
          return;
        }
      }
    });

    httpHandler = new GatewayHttpHandler(routeLocator, filterRegistry);
    HttpRequestRouter router = TioBootServer.me().getRequestRouter();
    router.any("/**", httpHandler);
    if (EnvUtils.getBoolean("gateway.websocket.enabled", true)) {
      TioBootServer.me().getWebSocketRouter().add("/**",
          new WebSocketProxyHandler(new GatewayWebSocketResolver(routeLocator, filterRegistry)));
    }
  }

  /**
   * Sets the response for requests the upstream cannot answer. Call after
   * {@link #start()}.
   */
  public static void setFallback(GatewayFallback fallback) {
    httpHandler.setFallback(fallback);
  }

  public static void addGlobalFilter(GlobalFilter filter) {
    filterRegistry.add(filter);
  }

  public static void addRoute(RouteDefinition definition) {
    routeLocator.add(definition);
  }

  public static RouteLocator getRouteLocator() {
    return routeLocator;
  }
}
