package nexus.io.tio.boot.cloud.gateway.filter;

import nexus.io.tio.boot.cloud.gateway.GatewayExchange;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.utils.environment.EnvUtils;

/**
 * Answers CORS preflight requests and adds CORS headers to every response. The
 * request's Origin is echoed back, so credentials work from any origin; set
 * {@code gateway.cors.allowed-origins} (comma separated) to restrict it.
 */
public class CorsGlobalFilter implements GlobalFilter {

  @Override
  public int getOrder() {
    return Integer.MIN_VALUE;
  }

  @Override
  public HttpResponse filter(GatewayExchange exchange, GatewayFilterChain chain) throws Exception {
    String origin = exchange.getRequestHeader("origin");
    if (origin == null || exchange.isWebSocket()) {
      return chain.filter(exchange);
    }
    if (!allowed(origin)) {
      return chain.filter(exchange);
    }
    HttpResponse response = exchange.getResponse();
    exchange.setResponseHeader("Access-Control-Allow-Origin", origin);
    exchange.setResponseHeader("Access-Control-Allow-Credentials", "true");
    exchange.setResponseHeader("Vary", "Origin");
    if ("OPTIONS".equals(exchange.getMethod()) && exchange.getRequestHeader("access-control-request-method") != null) {
      String headers = exchange.getRequestHeader("access-control-request-headers");
      exchange.setResponseHeader("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS,HEAD");
      exchange.setResponseHeader("Access-Control-Allow-Headers", headers == null ? "*" : headers);
      exchange.setResponseHeader("Access-Control-Max-Age", EnvUtils.get("gateway.cors.max-age", "18000"));
      response.setStatus(200);
      return response;
    }
    return chain.filter(exchange);
  }

  private static boolean allowed(String origin) {
    String allowed = EnvUtils.get("gateway.cors.allowed-origins");
    if (allowed == null || allowed.trim().isEmpty() || "*".equals(allowed.trim())) {
      return true;
    }
    for (String item : allowed.split(",")) {
      if (item.trim().equalsIgnoreCase(origin)) {
        return true;
      }
    }
    return false;
  }
}
