package nexus.io.tio.boot.cloud.gateway;

import nexus.io.tio.http.common.HttpResponse;

/**
 * Builds the response when the upstream cannot answer: the circuit breaker is
 * open, no instance is available or the connection failed.
 */
public interface GatewayFallback {

  /**
   * @param exchange the request
   * @param status   the status the gateway would answer with otherwise: 502 or
   *                 503
   * @param message  the reason
   * @return the response
   */
  HttpResponse handle(GatewayExchange exchange, int status, String message);
}
