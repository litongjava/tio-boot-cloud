package nexus.io.tio.boot.cloud.demo.gateway.filter;

import nexus.io.model.body.RespBodyVo;
import nexus.io.tio.boot.cloud.gateway.GatewayExchange;
import nexus.io.tio.boot.cloud.gateway.filter.GatewayFilterChain;
import nexus.io.tio.boot.cloud.gateway.filter.GlobalFilter;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.utils.environment.EnvUtils;

/**
 * Rejects tokens listed in demo.blocked-tokens (comma separated) for HTTP and
 * WebSocket requests alike.
 */
public class BlockListFilter implements GlobalFilter {

  @Override
  public HttpResponse filter(GatewayExchange exchange, GatewayFilterChain chain) throws Exception {
    String token = tokenOf(exchange);
    String blocked = EnvUtils.get("demo.blocked-tokens", "");
    if (token != null && !token.isEmpty() && ("," + blocked + ",").contains("," + token + ",")) {
      HttpResponse response = exchange.getResponse();
      response.setStatus(401);
      return response.setJson(RespBodyVo.fail(401, "token blocked"));
    }
    return chain.filter(exchange);
  }

  /**
   * Reads the token header first. Browsers cannot set headers on EventSource and
   * WebSocket connections, so the token query parameter is accepted as well.
   */
  private String tokenOf(GatewayExchange exchange) {
    String token = exchange.getRequestHeader("token");
    if (token == null || token.isEmpty()) {
      token = exchange.getRequest().getParam("token");
    }
    return token;
  }
}
