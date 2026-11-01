package nexus.io.tio.boot.cloud.gateway;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import nexus.io.tio.boot.cloud.gateway.filter.DefaultGatewayFilterChain;
import nexus.io.tio.boot.cloud.gateway.route.Route;
import nexus.io.tio.boot.cloud.gateway.route.RouteDefinition;
import nexus.io.tio.boot.cloud.gateway.route.RouteLocator;
import nexus.io.tio.http.common.HttpMethod;
import nexus.io.tio.http.common.HttpRequest;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.http.common.RequestLine;
import nexus.io.tio.utils.environment.EnvUtils;

public class RouteTest {

  private static GatewayExchange exchange(String method, String path, String query) {
    HttpRequest request = new HttpRequest();
    RequestLine line = new RequestLine();
    line.setMethod(HttpMethod.from(method));
    line.setPath(path);
    line.setQueryString(query);
    request.setRequestLine(line);
    return new GatewayExchange(request, new HttpResponse(request), false);
  }

  private static GatewayExchange run(Route route, GatewayExchange exchange) throws Exception {
    exchange.setRoute(route);
    new DefaultGatewayFilterChain(new java.util.ArrayList<>(route.getFilters()), e -> e.getResponse()).filter(exchange);
    return exchange;
  }

  @Test
  public void stripPrefixKeepsQueryAndSetsForwardedPrefix() throws Exception {
    Route route = new Route(new RouteDefinition("im", "lb://im-server").predicate("Path=/im/**").filter("StripPrefix=1"));
    GatewayExchange exchange = exchange("GET", "/im/chat/page", "size=10");
    assertTrue(route.matches(exchange));
    run(route, exchange);
    assertEquals("/chat/page?size=10", exchange.getPathAndQuery());
    assertEquals("/im", exchange.getRequestHeader("x-forwarded-prefix"));
    assertTrue(route.isLoadBalanced());
    assertEquals("im-server", route.getServiceId());
  }

  @Test
  public void rewritePathAcceptsEscapedGroupReference() throws Exception {
    Route route = new Route(new RouteDefinition("r", "http://127.0.0.1:9000")
        .predicate("Path=/old/**").filter("RewritePath=/old/(?<rest>.*), /new/$\\{rest}"));
    GatewayExchange exchange = run(route, exchange("POST", "/old/a/b", null));
    assertEquals("/new/a/b", exchange.getPath());
    assertEquals("http://127.0.0.1:9000", route.getBaseUri());
  }

  @Test
  public void methodAndWebSocketRoutes() {
    Route ws = new Route(new RouteDefinition("ws", "lb:ws://ws-server").predicate("Path=/ws/**"));
    assertTrue(ws.isWebSocket());
    assertEquals("ws-server", ws.getServiceId());
    Route onlyGet = new Route(new RouteDefinition("g", "lb://s").predicate("Path=/x/**").predicate("Method=GET"));
    assertTrue(onlyGet.matches(exchange("GET", "/x/1", null)));
    assertFalse(onlyGet.matches(exchange("POST", "/x/1", null)));
  }

  @Test
  public void readsRoutesFromConfiguration() {
    EnvUtils.set("gateway.routes[0].id", "oauth");
    EnvUtils.set("gateway.routes[0].uri", "lb://oauth-server");
    EnvUtils.set("gateway.routes[0].predicates[0]", "Path=/oauth/**");
    EnvUtils.set("gateway.routes[0].filters[0]", "StripPrefix=1");
    EnvUtils.set("gateway.routes[1].id", "im");
    EnvUtils.set("gateway.routes[1].uri", "lb://im-server");
    EnvUtils.set("gateway.routes[1].predicates[0]", "Path=/im/**");
    EnvUtils.set("gateway.routes[1].order", "-1");
    List<RouteDefinition> definitions = RouteLocator.readConfig();
    assertEquals(2, definitions.size());
    RouteLocator locator = new RouteLocator();
    locator.refresh();
    assertEquals("im", locator.getRoutes().get(0).getId());
    assertEquals("oauth", locator.match(exchange("GET", "/oauth/login", null)).getId());
    assertNull(locator.match(exchange("GET", "/nothing", null)));
  }
}
