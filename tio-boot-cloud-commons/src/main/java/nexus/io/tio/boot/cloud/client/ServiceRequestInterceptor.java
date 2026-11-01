package nexus.io.tio.boot.cloud.client;

/**
 * Changes every outgoing service call, for example to pass on the caller's
 * token, tenant or trace id.
 */
@FunctionalInterface
public interface ServiceRequestInterceptor {

  void apply(String serviceId, ServiceRequest request);
}
