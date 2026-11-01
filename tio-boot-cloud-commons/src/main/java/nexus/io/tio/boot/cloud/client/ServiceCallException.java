package nexus.io.tio.boot.cloud.client;

/**
 * A service call failed: no instance was available or none could be reached.
 */
public class ServiceCallException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public ServiceCallException(String message) {
    super(message);
  }

  public ServiceCallException(String message, Throwable cause) {
    super(message, cause);
  }
}
