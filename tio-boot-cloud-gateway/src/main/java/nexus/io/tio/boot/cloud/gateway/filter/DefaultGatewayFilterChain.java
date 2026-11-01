package nexus.io.tio.boot.cloud.gateway.filter;

import java.util.List;

import nexus.io.tio.boot.cloud.gateway.GatewayExchange;
import nexus.io.tio.http.common.HttpResponse;

/**
 * Runs the filters in order, then the terminal step.
 */
public class DefaultGatewayFilterChain implements GatewayFilterChain {
  private final List<GatewayFilter> filters;
  private final GatewayFilterChain terminal;
  private final int index;

  public DefaultGatewayFilterChain(List<GatewayFilter> filters, GatewayFilterChain terminal) {
    this(filters, terminal, 0);
  }

  private DefaultGatewayFilterChain(List<GatewayFilter> filters, GatewayFilterChain terminal, int index) {
    this.filters = filters;
    this.terminal = terminal;
    this.index = index;
  }

  @Override
  public HttpResponse filter(GatewayExchange exchange) throws Exception {
    if (index < filters.size()) {
      return filters.get(index).filter(exchange, new DefaultGatewayFilterChain(filters, terminal, index + 1));
    }
    return terminal.filter(exchange);
  }
}
