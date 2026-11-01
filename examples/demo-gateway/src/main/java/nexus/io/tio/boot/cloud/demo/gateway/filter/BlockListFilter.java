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
    String token = exchange.getRequestHeader("token");
    String blocked = EnvUtils.get("demo.blocked-tokens", "");
    if (token != null && !token.isEmpty() && ("," + blocked + ",").contains("," + token + ",")) {
      HttpResponse response = exchange.getResponse();
      response.setStatus(401);
      return response.setJson(RespBodyVo.fail(401, "token blocked"));
    }
    return chain.filter(exchange);
  }
}
