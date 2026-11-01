package nexus.io.tio.boot.cloud.gateway.filter;

import nexus.io.tio.boot.cloud.gateway.GatewayExchange;
import nexus.io.tio.http.common.HttpResponse;
import nexus.io.tio.utils.environment.EnvUtils;
import nexus.io.tio.utils.snowflake.SnowflakeIdUtils;

/**
 * Gives every request a trace id header ({@code gateway.trace.header}, default
 * {@code x-trace-id}) unless the client already sent one. The id is passed to
 * the upstream and returned to the client.
 */
public class TraceIdGlobalFilter implements GlobalFilter {

  @Override
  public int getOrder() {
    return Integer.MIN_VALUE + 1;
  }

  @Override
  public HttpResponse filter(GatewayExchange exchange, GatewayFilterChain chain) throws Exception {
    String header = EnvUtils.get("gateway.trace.header", "x-trace-id");
    String traceId = exchange.getRequestHeader(header);
    if (traceId == null || traceId.isEmpty()) {
      traceId = String.valueOf(SnowflakeIdUtils.id());
      exchange.setRequestHeader(header, traceId);
    }
    if (!exchange.isWebSocket()) {
      exchange.setResponseHeader(header, traceId);
    }
    return chain.filter(exchange);
  }
}
