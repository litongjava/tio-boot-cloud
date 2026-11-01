package nexus.io.tio.boot.cloud.gateway.filter;

import nexus.io.tio.boot.cloud.gateway.GatewayExchange;
import nexus.io.tio.http.common.HttpResponse;

/**
 * Works on a request before it is forwarded. Call {@code chain.filter(exchange)}
 * to continue, or return a response to answer the client directly (for
 * example 401 when the token is missing).
 */
@FunctionalInterface
public interface GatewayFilter {

  HttpResponse filter(GatewayExchange exchange, GatewayFilterChain chain) throws Exception;
}
