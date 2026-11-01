package nexus.io.tio.boot.cloud.gateway;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nexus.io.tio.boot.cloud.CloudContext;
import nexus.io.tio.boot.cloud.discovery.ServiceInstance;
import nexus.io.tio.boot.cloud.gateway.filter.DefaultGatewayFilterChain;
import nexus.io.tio.boot.cloud.gateway.route.Route;
import nexus.io.tio.boot.cloud.gateway.route.RouteLocator;
import nexus.io.tio.boot.cloud.loadbalancer.LoadBalancerRequest;
import nexus.io.tio.boot.http.TioRequestContext;
import nexus.io.tio.boot.http.forward.TioHttpProxy;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.server.handler.HttpRequestHandler;
import nexus.io.tio.utils.environment.EnvUtils;
import nexus.io.tio.utils.http.OkHttpClientPool;
import okhttp3.OkHttpClient;

/**
 * Handles HTTP requests of the gateway: finds the route, runs the filters,
 * picks an instance and forwards the request. Server-Sent Events and other
 * streamed responses are relayed as they arrive.
 * <p>
 * Configuration:
 * <ul>
 * <li>{@code gateway.http.connect-timeout}: seconds, default 3</li>
 * <li>{@code gateway.http.read-timeout}: seconds, default 300, long enough for
 * streamed AI answers</li>
 * <li>{@code gateway.http.max-attempts}: instances to try when one cannot be
 * reached, default 2</li>
 * </ul>
 */
public class GatewayHttpHandler implements HttpRequestHandler {
  private static final Logger log = LoggerFactory.getLogger(GatewayHttpHandler.class);

  private final RouteLocator routeLocator;
  private final GatewayFilterRegistry filterRegistry;
  private final OkHttpClient httpClient;
  private final int maxAttempts;

  public GatewayHttpHandler(RouteLocator routeLocator, GatewayFilterRegistry filterRegistry) {
    this.routeLocator = routeLocator;
    this.filterRegistry = filterRegistry;
    this.httpClient = OkHttpClientPool.getHttpClient().newBuilder()
        .connectTimeout(EnvUtils.getInt("gateway.http.connect-timeout", 3), TimeUnit.SECONDS)
        .readTimeout(EnvUtils.getInt("gateway.http.read-timeout", 300), TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build();
    this.maxAttempts = Math.max(1, EnvUtils.getInt("gateway.http.max-attempts", 2));
  }

  @Override
  public HttpResponse handle(HttpRequest request) throws Exception {
    HttpResponse response = TioRequestContext.getResponse();
    GatewayExchange exchange = new GatewayExchange(request, response, false);
    Route route = routeLocator.match(exchange);
    if (route == null) {
      return GatewayResponses.error(response, 404, "No route for " + request.getRequestLine().getPath());
    }
    exchange.setRoute(route);
    return new DefaultGatewayFilterChain(filterRegistry.filtersFor(route), this::forward).filter(exchange);
  }

  private HttpResponse forward(GatewayExchange exchange) {
    Route route = exchange.getRoute();
    HttpRequest request = exchange.getRequest();
    HttpResponse response = exchange.getResponse();
    if (!route.isLoadBalanced()) {
      try {
        return TioHttpProxy.forward(httpClient, route.getBaseUri() + exchange.getPathAndQuery(), request, response,
            null);
      } catch (IOException e) {
        log.error("forward to {} failed: {}", route.getBaseUri(), e.getMessage());
        return GatewayResponses.error(response, 502, "Bad Gateway");
      }
    }

    LoadBalancerRequest lbRequest = new LoadBalancerRequest(request.getHeaders());
    List<ServiceInstance> tried = new ArrayList<>();
    for (int attempt = 0; attempt < maxAttempts; attempt++) {
      ServiceInstance instance = CloudContext.choose(route.getServiceId(), lbRequest, tried);
      if (instance == null) {
        break;
      }
      tried.add(instance);
      String url = instance.getUri() + exchange.getPathAndQuery();
      try {
        return TioHttpProxy.forward(httpClient, url, request, response, null);
      } catch (ConnectException | NoRouteToHostException e) {
        log.warn("{} unreachable, trying another instance: {}", instance, e.getMessage());
      } catch (IOException e) {
        log.error("forward to {} failed: {}", url, e.getMessage());
        return GatewayResponses.error(response, 502, "Bad Gateway");
      }
    }
    if (tried.isEmpty()) {
      return GatewayResponses.error(response, 503, "No available instance of " + route.getServiceId());
    }
    return GatewayResponses.error(response, 502, "Bad Gateway");
  }
}
