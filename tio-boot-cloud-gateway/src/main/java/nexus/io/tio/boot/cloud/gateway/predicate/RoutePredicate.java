package nexus.io.tio.boot.cloud.gateway.predicate;

import nexus.io.tio.boot.cloud.gateway.GatewayExchange;

/**
 * Decides whether a request belongs to a route.
 */
@FunctionalInterface
public interface RoutePredicate {

  boolean test(GatewayExchange exchange);
}
