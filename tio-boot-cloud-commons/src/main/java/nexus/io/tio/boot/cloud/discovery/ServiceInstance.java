package nexus.io.tio.boot.cloud.discovery;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One running instance of a service.
 */
public class ServiceInstance {
  private String serviceId;
  private String instanceId;
  private String host;
  private int port;
  private boolean secure;
  private double weight = 1.0;
  private Map<String, String> metadata = new LinkedHashMap<>();

  public ServiceInstance() {
  }

  public ServiceInstance(String serviceId, String host, int port) {
    this.serviceId = serviceId;
    this.host = host;
    this.port = port;
    this.instanceId = host + ":" + port;
  }

  /**
   * @return {@code http://host:port} or {@code https://host:port}
   */
  public String getUri() {
    return (secure ? "https://" : "http://") + hostForUri() + ":" + port;
  }

  /**
   * @return {@code ws://host:port} or {@code wss://host:port}
   */
  public String getWebSocketUri() {
    return (secure ? "wss://" : "ws://") + hostForUri() + ":" + port;
  }

  private String hostForUri() {
    if (host != null && host.indexOf(':') >= 0 && !host.startsWith("[")) {
      return "[" + host + "]";
    }
    return host;
  }

  public String getMetadata(String key) {
    return metadata == null ? null : metadata.get(key);
  }

  public String getServiceId() {
    return serviceId;
  }

  public ServiceInstance setServiceId(String serviceId) {
    this.serviceId = serviceId;
    return this;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public ServiceInstance setInstanceId(String instanceId) {
    this.instanceId = instanceId;
    return this;
  }

  public String getHost() {
    return host;
  }

  public ServiceInstance setHost(String host) {
    this.host = host;
    return this;
  }

  public int getPort() {
    return port;
  }

  public ServiceInstance setPort(int port) {
    this.port = port;
    return this;
  }

  public boolean isSecure() {
    return secure;
  }

  public ServiceInstance setSecure(boolean secure) {
    this.secure = secure;
    return this;
  }

  public double getWeight() {
    return weight;
  }

  public ServiceInstance setWeight(double weight) {
    this.weight = weight;
    return this;
  }

  public Map<String, String> getMetadata() {
    return metadata == null ? Collections.<String, String>emptyMap() : metadata;
  }

  public ServiceInstance setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
    return this;
  }

  @Override
  public String toString() {
    return serviceId + "@" + host + ":" + port;
  }
}
