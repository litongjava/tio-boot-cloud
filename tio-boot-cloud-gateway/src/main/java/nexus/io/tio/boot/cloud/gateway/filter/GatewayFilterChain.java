package nexus.io.tio.boot.cloud.gateway.filter;

import nexus.io.tio.boot.cloud.gateway.GatewayExchange;
import nexus.io.tio.http.common.HttpResponse;

/**
 * Passes the exchange to the next filter, and finally to the proxy.
 */
public interface GatewayFilterChain {

  HttpResponse filter(GatewayExchange exchange) throws Exception;
}
