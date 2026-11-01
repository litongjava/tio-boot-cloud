package nexus.io.tio.boot.cloud.gateway.filter;

/**
 * A filter that applies to every route. Global filters run before route
 * filters, lowest order first.
 */
public interface GlobalFilter extends GatewayFilter {

  default int getOrder() {
    return 0;
  }
}
